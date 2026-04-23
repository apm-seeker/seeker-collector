package com.seeker.collector.kafka.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AgentEventDto {
    private AgentEventType agentEventType;
    private UUID agentId;
    private Object payload;
}
