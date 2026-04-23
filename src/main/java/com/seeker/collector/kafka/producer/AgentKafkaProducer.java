package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.EventType;
import com.seeker.collector.kafka.dto.payload.AgentCreatedPayload;
import com.seeker.collector.kafka.dto.payload.AgentDeletedPayload;
import com.seeker.collector.kafka.dto.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentKafkaProducer {

    private static final String AGENT_TOPIC = "agent-events";

    private final KafkaEventPublisher eventPublisher;

    public Mono<Void> sendCreatedEvent(AgentCreatedPayload payload) {
        return sendEvent(EventType.AGENT_CREATED, payload.getAgentId(), payload);
    }

    public Mono<Void> sendDeletedEvent(AgentDeletedPayload payload) {
        return sendEvent(EventType.AGENT_DELETED, payload.getAgentId(), payload);
    }

    private <T> Mono<Void> sendEvent(EventType eventType, String key, T payload) {

        EventEnvelope<T> event = EventEnvelope.<T>builder()
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();

        return eventPublisher.publish(AGENT_TOPIC, key, event);
    }
}
