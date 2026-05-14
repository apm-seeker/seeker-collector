package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.EventEnvelope;
import com.seeker.collector.kafka.dto.EventType;
import com.seeker.collector.kafka.dto.payload.metric.MetricSnapshotPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricKafkaProducer {

    private static final String METRIC_TOPIC = "metric";

    private final KafkaEventPublisher eventPublisher;

    public Mono<Void> sendMetricSnapshot(MetricSnapshotPayload payload, String agentId) {
        return sendEvent(EventType.METRIC_SNAPSHOT, agentId, payload);
    }

    private <T> Mono<Void> sendEvent(EventType eventType, String key, T payload) {

        EventEnvelope<T> event = EventEnvelope.<T>builder()
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();

        return eventPublisher.publish(METRIC_TOPIC, key, event);
    }

}
