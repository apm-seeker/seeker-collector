package com.seeker.collector.otlp;

import com.seeker.collector.kafka.dto.payload.AgentCreatedPayload;
import com.seeker.collector.kafka.dto.payload.SpanPayload;
import com.seeker.collector.kafka.dto.payload.TracePayload;
import com.seeker.collector.kafka.producer.AgentKafkaProducer;
import com.seeker.collector.kafka.producer.TraceDataKafkaProducer;
import com.seeker.collector.otlp.proto.collector.trace.v1.ExportTraceServiceRequest;
import com.seeker.collector.otlp.proto.collector.trace.v1.ExportTraceServiceResponse;
import com.seeker.collector.otlp.proto.collector.trace.v1.TraceServiceGrpc;
import com.seeker.collector.otlp.proto.trace.v1.ResourceSpans;
import com.seeker.collector.otlp.proto.trace.v1.ScopeSpans;
import com.seeker.collector.otlp.proto.trace.v1.Span;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OTLP/gRPC 의 TraceService 구현체. Envoy / OTel SDK / OTel Collector 등이 이 서비스로
 * trace 데이터를 보낸다.
 *
 * <p>gRPC wire 경로: <code>/opentelemetry.proto.collector.trace.v1.TraceService/Export</code>
 *
 * <p>{@link TraceServiceGrpc.TraceServiceImplBase} 는 protoc 가 trace_service.proto 로부터
 * 생성한 추상 서버 클래스. {@code export} 메서드만 오버라이드하면 된다.
 *
 * <p>흐름:
 * <ol>
 *   <li>요청에 들어있는 ResourceSpans 들을 순회</li>
 *   <li>각 ResourceSpans 의 Resource 에서 agentId 추출 (service.name+host.name 해시)</li>
 *   <li>처음 보는 agentId 면 AGENT_CREATED 이벤트를 Kafka 에 발행 (1회)</li>
 *   <li>각 Span 을 SpanPayload 로 변환해 Kafka 에 발행</li>
 *   <li>root span 이면 TracePayload 도 함께 발행</li>
 *   <li>응답으로 빈 ExportTraceServiceResponse (=전체 성공) 반환</li>
 * </ol>
 *
 * <p>주의: Kafka 발행은 비동기 fire-and-forget. 발행 실패해도 클라이언트에는 성공으로 응답한다
 * (응답 시점엔 결과를 모르므로). 발행 실패는 로그로만 남김.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtlpTraceService extends TraceServiceGrpc.TraceServiceImplBase {

    private final OtlpSpanMapper mapper;                         // OTLP → 내부 페이로드 변환
    private final SeenAgentCache seenAgentCache;                 // agent 첫 등장 추적
    private final TraceDataKafkaProducer traceDataKafkaProducer; // SPAN/TRACE 발행
    private final AgentKafkaProducer agentKafkaProducer;         // AGENT_CREATED 발행

    /**
     * unary RPC — 한 번의 호출에 batch 데이터가 들어오고 한 번 응답한다.
     *
     * @param request          ResourceSpans 들의 묶음 (보통 Envoy 1 인스턴스 = 1개)
     * @param responseObserver gRPC 응답 채널. onNext 1회 + onCompleted() 로 종료.
     */
    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {

        // 한 Export 요청 = 여러 ResourceSpans 가능 (collector 체인에서 여러 출처를 forward 시).
        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            // Resource attribute (service.name, host.name) → agentId 결정
            OtlpSpanMapper.ResourceContext ctx = mapper.extractResource(resourceSpans.getResource());

            // 이 콜렉터 프로세스에서 처음 보는 agentId 면 AGENT_CREATED 이벤트 발행
            if (seenAgentCache.markIfFirst(ctx.agentId())) {
                AgentCreatedPayload agent = mapper.toAgentCreatedPayload(ctx);
                agentKafkaProducer.sendCreatedEvent(agent)
                        // fire-and-forget: 실패해도 다음 처리에 영향 없음
                        .subscribe(null, err -> log.error(
                                "[OTLP] AGENT_CREATED 발행 실패 - agentId: {}", ctx.agentId(), err));
            }

            // ScopeSpans = 같은 계측 라이브러리가 만든 span 묶음 (Envoy 는 보통 1개)
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    publishSpan(span, ctx);
                }
            }
        }

        // OTLP 표준: 빈 ExportTraceServiceResponse = "전체 성공". partial_success 가 비어있음.
        responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /**
     * 한 Span 을 변환해 Kafka 에 발행.
     * - 항상 SPAN 발행
     * - root span (parent_span_id 가 비어있음) 이면 TRACE 도 함께 발행
     */
    private void publishSpan(Span span, OtlpSpanMapper.ResourceContext ctx) {
        SpanPayload spanPayload = mapper.toSpanPayload(span, ctx);
        // Kafka 키로 traceId 사용 — 같은 trace 의 모든 메시지가 동일 파티션에 들어가 순서 보존
        String traceId = spanPayload.getTraceId();

        traceDataKafkaProducer.sendSpan(spanPayload, traceId)
                .subscribe(null, err -> log.error(
                        "[OTLP] SPAN 발행 실패 - traceId: {}", traceId, err));

        if (mapper.isRoot(span)) {
            TracePayload tracePayload = mapper.toTracePayload(span);
            traceDataKafkaProducer.sendTrace(tracePayload, traceId)
                    .subscribe(null, err -> log.error(
                            "[OTLP] TRACE 발행 실패 - traceId: {}", traceId, err));
        }
    }
}
