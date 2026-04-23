package com.seeker.collector.agent.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentCreateRequest {
    private String agentId;
    private String agentName;
    private String agentType;
    private String agentGroup;
    private Long startTime;
}
