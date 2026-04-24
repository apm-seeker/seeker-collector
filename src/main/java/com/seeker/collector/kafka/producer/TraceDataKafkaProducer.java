package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.EventEnvelope;
import com.seeker.collector.kafka.dto.EventType;
import com.seeker.collector.kafka.dto.payload.SpanEventPayload;
import com.seeker.collector.kafka.dto.payload.SpanPayload;
import com.seeker.collector.kafka.dto.payload.TracePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceDataKafkaProducer {

    private static final String TRACE_DATA_TOPIC = "trace-data";

    private final KafkaEventPublisher eventPublisher;

    public Mono<Void> sendTrace(TracePayload payload, String traceId) {
        return sendEvent(EventType.TRACE, traceId, payload);
    }

    public Mono<Void> sendSpan(SpanPayload payload, String traceId) {
        return sendEvent(EventType.SPAN, traceId, payload);
    }

    public Mono<Void> sendSpanEvent(SpanEventPayload payload, String traceId) {
        return sendEvent(EventType.SPAN_EVENT, traceId, payload);
    }

    private <T> Mono<Void> sendEvent(EventType eventType, String key, T payload) {

        EventEnvelope<T> event = EventEnvelope.<T>builder()
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();

        return eventPublisher.publish(TRACE_DATA_TOPIC, key, event);
    }

}
