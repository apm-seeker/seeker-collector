package com.seeker.collector.kafka.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventEnvelope<T> {

    private EventType eventType;
    private Long timestamp;

    private T payload;

}
