package com.seeker.collector.otlp;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * "이 agentId 가 이번 콜렉터 프로세스 생애에 처음 보였는가?" 를 추적하는 in-memory 캐시.
 *
 * <p>OTLP 클라이언트는 agent 등록 절차(REST POST /agents) 를 거치지 않으므로 콜렉터가
 * 첫 데이터 수신 시점에 자동으로 AGENT_CREATED 이벤트를 발행해야 한다. 매번 발행하면
 * 다운스트림이 같은 agent 로 중복 INSERT 처리해야 하므로, "처음일 때만 1회" 로 제한한다.
 *
 * <p>특성:
 * <ul>
 *   <li>in-memory only — 콜렉터 재시작 시 모두 잊고 처음부터 다시 발행</li>
 *   <li>다운스트림(Kafka 컨슈머)이 멱등성을 보장해야 안전 (같은 agentId 로 들어오면 upsert)</li>
 *   <li>thread-safe — ConcurrentHashMap 의 putIfAbsent 가 원자적</li>
 * </ul>
 */
@Component
public class SeenAgentCache {

    // value 는 의미 없는 sentinel. Set 대용으로 ConcurrentHashMap 사용.
    // ConcurrentHashMap.newKeySet() 은 add 가 원자적이지만 "처음인지" 확인은 별도 호출이 필요해서
    // putIfAbsent 의 "이전 값" 반환을 활용하는 쪽이 race 없이 깔끔.
    private final ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<>();

    /**
     * 주어진 agentId 가 이 인스턴스에서 처음 등장했으면 true, 그 외엔 false.
     *
     * <p>putIfAbsent 의 의미:
     * <ul>
     *   <li>key 가 없으면 → 값 저장 후 null 반환 → 우리 함수는 true (= "처음")</li>
     *   <li>key 가 있으면 → 기존 값 반환(non-null) → 우리 함수는 false</li>
     * </ul>
     *
     * <p>여러 스레드가 동시에 같은 agentId 로 호출해도 정확히 1개 호출만 true 를 받음.
     */
    public boolean markIfFirst(String agentId) {
        return seen.putIfAbsent(agentId, Boolean.TRUE) == null;
    }
}
