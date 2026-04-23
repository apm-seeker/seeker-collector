package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.AgentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentKafkaProducer {

    private static final String AGENT_TOPIC = "agent-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // agent 이벤트는 ack 동기 보장
    public Mono<Void> sendAgent(AgentEventDto dto) {
        return Mono.fromFuture(
                        kafkaTemplate.send(AGENT_TOPIC, dto.getAgentId().toString(), dto)
                )
                .doOnError(ex ->
                        log.error("[Collector] kafka agent event 전송 실패: agentId={}, eventType={}",
                                dto.getAgentId(),
                                dto.getAgentEventType(),
                                ex)
                )
                .then();
    }

}
