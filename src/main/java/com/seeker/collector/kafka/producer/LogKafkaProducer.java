package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.EventEnvelope;
import com.seeker.collector.kafka.dto.EventType;
import com.seeker.collector.kafka.dto.payload.LogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogKafkaProducer {

    private static final String LOG_TOPIC = "log";

    private final KafkaEventPublisher eventPublisher;

    public Mono<Void> sendLog(LogPayload payload) {
        return sendEvent(resolveKey(payload), payload);
    }

    private Mono<Void> sendEvent(String key, LogPayload payload) {

        EventEnvelope<LogPayload> event = EventEnvelope.<LogPayload>builder()
                .eventType(EventType.LOG)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();

        return eventPublisher.publish(LOG_TOPIC, key, event);
    }

    private String resolveKey(LogPayload payload) {
        if (payload.getTraceId() != null && !payload.getTraceId().isBlank()) {
            return payload.getTraceId();
        }
        return payload.getAgentId();
    }
}
