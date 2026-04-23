package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentDeletedPayload implements AgentPayload {

    private String agentId;
    private Long endTime;

}
