package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.SpanDto;
import com.seeker.collector.kafka.dto.SpanEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpanKafkaProducer {

    private static final String SPAN_TOPIC = "span";
    private static final String SPAN_EVENT_TOPIC = "span-event";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSpan(SpanDto spanDto) {
        kafkaTemplate.send(SPAN_TOPIC, spanDto.getTraceId(), spanDto)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Collector] Kafka Span 전송 실패: traceId={}", spanDto.getTraceId(), ex);
                    }
                });
    }

    public void sendSpanEvent(SpanEventDto spanEventDto) {
        kafkaTemplate.send(SPAN_EVENT_TOPIC, spanEventDto.getTraceId(), spanEventDto)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Collector] Kafka SpanEvent 전송 실패: traceId={}", spanEventDto.getTraceId(), ex);
                    }
                });
    }
}
