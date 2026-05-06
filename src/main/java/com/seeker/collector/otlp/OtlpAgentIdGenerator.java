package com.seeker.collector.otlp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * OTLP 로 들어온 데이터에 부여할 agentId 를 생성하는 유틸.
 *
 * <p>배경: 자체 프로토콜은 agent 가 직접 agentId 를 들고 등록(REST POST /agents)하지만,
 * OTLP 클라이언트(Envoy 등)는 agentId 개념이 없음. 대신 Resource attribute 에 service.name,
 * host.name 이 들어 있으므로 이 둘을 조합해서 결정적(deterministic) agentId 를 만들어낸다.
 *
 * <p>결정적 ID 의 효과:
 * <ul>
 *   <li>같은 (service.name, host.name) → 항상 같은 agentId — 콜렉터 재시작에도 동일</li>
 *   <li>여러 콜렉터 인스턴스가 분산 처리해도 결과가 일관됨 (분산 lock/DB 조회 불필요)</li>
 *   <li>다운스트림에서는 agentId 만 보고도 "같은 agent" 로 묶을 수 있음</li>
 * </ul>
 */
public final class OtlpAgentIdGenerator {

    // 인스턴스화 금지 — 정적 메서드만 제공
    private OtlpAgentIdGenerator() {
    }

    /**
     * service.name 과 host.name 을 조합한 SHA-256 해시의 앞 16 byte 를 hex 로 인코딩하여 반환.
     *
     * <p>결과: 32-char lowercase hex (= 128 bit). seeker 의 traceId 길이와 동일 포맷.
     *
     * @param serviceName Resource attribute "service.name" (없으면 호출 측이 기본값 처리)
     * @param hostName    Resource attribute "host.name" (없으면 호출 측이 기본값 처리)
     */
    public static String generate(String serviceName, String hostName) {
        // null 방어 — Resource attribute 가 빠져 있어도 NPE 안 나도록
        String key = (serviceName == null ? "" : serviceName)
                + "|"  // 구분자 — service.name 과 host.name 의 경계 표시
                + (hostName == null ? "" : hostName);

        try {
            // SHA-256: 256-bit 해시. 충돌 저항 강하고 JDK 표준 제공.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));

            // 256-bit 전부 쓸 필요 없음 — 앞 128-bit 만 잘라 32-char 로 만들면 traceId 와 자릿수 통일.
            // 128-bit 충돌 확률은 birthday bound 로 약 2^64 개 입력에서 1회 → 사실상 안전.
            byte[] truncated = new byte[16];
            System.arraycopy(hash, 0, truncated, 0, 16);

            // HexFormat.of() = 소문자, 구분자 없음. W3C / OTel 의 hex 표기 컨벤션과 일치.
            return HexFormat.of().formatHex(truncated);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 JDK 표준이라 절대 발생하지 않지만 checked exception 이라 catch 필요
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
