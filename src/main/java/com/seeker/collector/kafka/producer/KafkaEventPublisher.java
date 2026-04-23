package com.seeker.collector.kafka.producer;

import com.seeker.collector.kafka.dto.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> Mono<Void> publish(String topic, String key, EventEnvelope<T> event) {

        return Mono.fromFuture(kafkaTemplate.send(topic, key, event))
                .doOnError(ex ->
                        log.error("[Kafka] event 전송 실패 topic={}, key={}, eventType={}",
                                topic,
                                key,
                                event.getEventType(),
                                ex)
                )
                .then();
    }
}
