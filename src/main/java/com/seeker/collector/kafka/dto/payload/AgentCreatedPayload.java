package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentCreatedPayload implements AgentPayload {

    private String agentId;
    private String agentName;
    private String agentType;
    private String agentGroup;
    private Long startTime;

}
