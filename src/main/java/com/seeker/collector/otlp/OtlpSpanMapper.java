package com.seeker.collector.otlp;

import com.google.protobuf.ByteString;
import com.seeker.collector.kafka.dto.payload.AgentCreatedPayload;
import com.seeker.collector.kafka.dto.payload.SpanPayload;
import com.seeker.collector.kafka.dto.payload.TracePayload;
import com.seeker.collector.otlp.proto.common.v1.AnyValue;
import com.seeker.collector.otlp.proto.common.v1.KeyValue;
import com.seeker.collector.otlp.proto.resource.v1.Resource;
import com.seeker.collector.otlp.proto.trace.v1.Span;
import com.seeker.collector.otlp.proto.trace.v1.Status;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OTLP proto 메시지(Resource / Span)를 seeker 내부 페이로드(SpanPayload / TracePayload /
 * AgentCreatedPayload)로 변환하는 매퍼.
 *
 * <p>이 클래스는 부수 효과가 없는 순수 변환기 — Kafka 발행/캐시 갱신 같은 외부 상호작용은
 * 하지 않고, 호출 측({@link OtlpTraceService})이 그 책임을 진다.
 *
 * <p>핵심 변환 규칙:
 * <table>
 *   <tr><th>OTLP 입력</th><th>seeker 출력</th></tr>
 *   <tr><td>Resource attrs (service.name, host.name)</td><td>agentId (SHA-256 해시)</td></tr>
 *   <tr><td>Span.trace_id (16 bytes)</td><td>traceId (32-char hex)</td></tr>
 *   <tr><td>Span.span_id (8 bytes)</td><td>spanId (long, 64-bit reinterpret)</td></tr>
 *   <tr><td>Span.parent_span_id (empty=root)</td><td>parentSpanId (-1 if root)</td></tr>
 *   <tr><td>start/end_time_unix_nano</td><td>startTime/elapsedTime (millis)</td></tr>
 *   <tr><td>attrs[http.target/url.path/...]</td><td>uri (semconv 신/구 폴백)</td></tr>
 *   <tr><td>Status.code==ERROR ? Status.message : ""</td><td>exceptionInfo</td></tr>
 * </table>
 */
@Component
public class OtlpSpanMapper {

    private static final String UNKNOWN_SERVICE = "unknown";
    private static final String EMPTY = "";

    // -------------------------------------------------------------------------
    // Resource 추출
    // -------------------------------------------------------------------------

    /**
     * Resource 의 attribute 목록에서 service.name / host.name / host.ip 를 뽑아
     * 결정적 agentId 와 함께 {@link ResourceContext} 로 묶어 반환.
     */
    public ResourceContext extractResource(Resource resource) {
        // Resource 가 비어있는 경우(스펙상 거의 없지만)도 안전하게 처리
        Map<String, String> attrs = (resource == null)
                ? Map.of()
                : flatten(resource.getAttributesList());

        // service.name 이 비면 OTel 표준 기본값 "unknown" 사용
        String serviceName = attrs.getOrDefault("service.name", UNKNOWN_SERVICE);
        String hostName = attrs.getOrDefault("host.name", EMPTY);

        // host.ip 는 OTel semconv 신/구 버전에 따라 키 이름이 다름
        String hostIp = firstNonEmpty(attrs.get("host.ip"), attrs.get("net.host.ip"));

        // (service.name, host.name) → 결정적 agentId
        String agentId = OtlpAgentIdGenerator.generate(serviceName, hostName);
        return new ResourceContext(agentId, serviceName, hostName, hostIp);
    }

    // -------------------------------------------------------------------------
    // AgentCreated 페이로드 (첫 등장 시 1회만 발행됨)
    // -------------------------------------------------------------------------

    public AgentCreatedPayload toAgentCreatedPayload(ResourceContext ctx) {
        return AgentCreatedPayload.builder()
                .agentId(ctx.agentId())
                .agentName(ctx.serviceName())
                // OTLP 경로로 들어온 agent 임을 다운스트림이 식별할 수 있도록 type 마커
                .agentType("OTLP")
                .agentGroup(EMPTY)
                // OTel 에는 "agent 시작 시각" 개념이 없으므로 첫 수신 시각으로 대체
                .startTime(System.currentTimeMillis())
                .build();
    }

    // -------------------------------------------------------------------------
    // Span → SpanPayload
    // -------------------------------------------------------------------------

    public SpanPayload toSpanPayload(Span span, ResourceContext ctx) {
        Map<String, String> attrs = flatten(span.getAttributesList());

        // ns → ms 변환 (정수 나눗셈으로 sub-ms 정보는 손실 — 우리 모델 단위가 ms 라 수용)
        long startMillis = span.getStartTimeUnixNano() / 1_000_000L;
        long endMillis = span.getEndTimeUnixNano() / 1_000_000L;
        // 비정상 데이터(end < start) 방어
        int elapsed = (int) Math.max(0L, endMillis - startMillis);

        // uri 추출 — OTel semconv 1.20(구) → 1.21+(신) 호환을 위해 여러 키를 순서대로 시도
        String uri = firstNonEmpty(
                attrs.get("http.target"),                // semconv 1.20
                attrs.get("url.path"),                   // semconv 1.21+
                attrs.get("http.url"),                   // legacy full URL
                attrs.get("url.full"));                  // semconv 1.21+ full URL

        // HTTP status code — 마찬가지로 신/구 키 폴백
        int statusCode = parseInt(firstNonEmpty(
                attrs.get("http.status_code"),                // semconv 1.20
                attrs.get("http.response.status_code")));     // semconv 1.21+

        // 호출자 IP
        String remoteAddr = firstNonEmpty(
                attrs.get("net.peer.ip"),
                attrs.get("client.address"),
                attrs.get("net.sock.peer.addr"));

        // 라우트(템플릿화된 path) 가 있으면 우선 — 없으면 span 이름 그대로
        String endPoint = firstNonEmpty(attrs.get("http.route"), span.getName());

        // OTel Status 는 OK/UNSET/ERROR 의 trichotomy
        // exceptionInfo 는 ERROR 일 때만 의미. SpanEvent("exception") 은 우리가 드롭하므로 stacktrace 는 못 받음.
        String exceptionInfo = EMPTY;
        if (span.hasStatus() && span.getStatus().getCode() == Status.StatusCode.STATUS_CODE_ERROR) {
            exceptionInfo = span.getStatus().getMessage();
        }

        return SpanPayload.builder()
                // 8 bytes → long (비트 보존, 음수 가능 — 다운스트림이 unsigned 로 다룬다고 가정)
                .spanId(toLong(span.getSpanId()))
                // root 판정: parent_span_id 가 빈 바이트열인지로 결정 (proto3 의 optional 부재 대응)
                .parentSpanId(span.getParentSpanId().isEmpty() ? -1L : toLong(span.getParentSpanId()))
                // 16 bytes → 32-char hex (W3C/OTel 표준 표기, seeker traceId 포맷과 동일)
                .traceId(toHex(span.getTraceId()))
                .agentId(ctx.agentId())
                // OTel 에는 "부모 agent" 개념이 없으므로 항상 빈 문자열
                .parentAgentId(EMPTY)
                .startTime(startMillis)
                .elapsedTime(elapsed)
                .uri(uri)
                .remoteAddress(remoteAddr)
                .endPoint(endPoint)
                .statusCode(statusCode)
                .exceptionInfo(exceptionInfo)
                .build();
    }

    // -------------------------------------------------------------------------
    // Span → TracePayload (root span 일 때만 호출됨)
    // -------------------------------------------------------------------------

    public TracePayload toTracePayload(Span span) {
        Map<String, String> attrs = flatten(span.getAttributesList());

        long startMillis = span.getStartTimeUnixNano() / 1_000_000L;
        long endMillis = span.getEndTimeUnixNano() / 1_000_000L;
        int elapsed = (int) Math.max(0L, endMillis - startMillis);

        String url = firstNonEmpty(
                attrs.get("http.target"),
                attrs.get("url.path"),
                attrs.get("http.url"),
                attrs.get("url.full"));

        int statusCode = parseInt(firstNonEmpty(
                attrs.get("http.status_code"),
                attrs.get("http.response.status_code")));

        String remoteAddr = firstNonEmpty(
                attrs.get("net.peer.ip"),
                attrs.get("client.address"),
                attrs.get("net.sock.peer.addr"));

        return TracePayload.builder()
                .traceId(toHex(span.getTraceId()))
                .startTime(startMillis)
                .elapsedTime(elapsed)
                .url(url)
                .remoteAddress(remoteAddr)
                .statusCode(statusCode)
                .build();
    }

    /**
     * root span 판정 — parent_span_id 가 빈 바이트열이면 trace 의 시작점.
     * proto3 는 optional 이 없어서 "없음" 을 length 0 으로 표현.
     */
    public boolean isRoot(Span span) {
        return span.getParentSpanId().isEmpty();
    }

    // -------------------------------------------------------------------------
    // 내부 유틸 — 바이트/속성/숫자 변환
    // -------------------------------------------------------------------------

    /**
     * 8 bytes 빅엔디안 → Java long (비트 보존, signed 로 reinterpret).
     * OTLP 의 span_id 는 항상 8 bytes 지만 비정상 입력 대비로 짧은 경우도 우측 정렬해서 처리.
     */
    private static long toLong(ByteString bytes) {
        if (bytes.size() < 8) {
            byte[] padded = new byte[8];
            byte[] src = bytes.toByteArray();
            // src 를 우측 정렬해서 0-padding (앞쪽이 0 으로 채워짐)
            System.arraycopy(src, 0, padded, 8 - src.length, src.length);
            return ByteBuffer.wrap(padded).getLong();
        }
        // 8 bytes 만 읽음 (혹시 더 길어도 앞 8 만 사용)
        return ByteBuffer.wrap(bytes.toByteArray(), 0, 8).getLong();
    }

    /** 바이트열 → lowercase hex (W3C 표기 규칙) */
    private static String toHex(ByteString bytes) {
        return HexFormat.of().formatHex(bytes.toByteArray());
    }

    /**
     * KeyValue 리스트 → Map&lt;String, String&gt; 평탄화.
     * AnyValue 의 다양한 타입을 모두 문자열로 통일 (우리 페이로드가 String 만 받음).
     */
    private static Map<String, String> flatten(List<KeyValue> list) {
        return list.stream()
                // 값이 set 안 된 KeyValue 는 무시
                .filter(kv -> kv.getValue().getValueCase() != AnyValue.ValueCase.VALUE_NOT_SET)
                .collect(Collectors.toMap(
                        KeyValue::getKey,
                        kv -> stringify(kv.getValue()),
                        // 같은 key 가 두 번 들어오면 첫 값 유지 (스펙상 중복은 비정상)
                        (a, b) -> a));
    }

    /** AnyValue oneof 를 사람이 읽을 수 있는 문자열로 직렬화 */
    private static String stringify(AnyValue v) {
        return switch (v.getValueCase()) {
            case STRING_VALUE -> v.getStringValue();
            case BOOL_VALUE -> Boolean.toString(v.getBoolValue());
            case INT_VALUE -> Long.toString(v.getIntValue());
            case DOUBLE_VALUE -> Double.toString(v.getDoubleValue());
            case BYTES_VALUE -> HexFormat.of().formatHex(v.getBytesValue().toByteArray());
            // ARRAY_VALUE / KVLIST_VALUE 는 평탄화하지 않고 빈 문자열 — 우리가 쓰는 키들은 모두 스칼라
            default -> EMPTY;
        };
    }

    /** 인자 중 첫 번째로 null/empty 가 아닌 값을 반환. 모두 비어있으면 "" */
    private static String firstNonEmpty(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isEmpty()) {
                return c;
            }
        }
        return EMPTY;
    }

    /** 문자열 → int (숫자 아니거나 비어있으면 0) — http.status_code 가 string 으로 오는 경우 대비 */
    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // ResourceContext — 한 ResourceSpans 에서 추출한 출처 정보
    // -------------------------------------------------------------------------
    /**
     * Resource 한 묶음에서 뽑아낸 식별 정보.
     * record 로 불변 + value-based equals/hashCode 자동 제공.
     */
    public record ResourceContext(String agentId, String serviceName, String hostName, String hostIp) {
    }
}
