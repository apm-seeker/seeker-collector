package com.seeker.collector.agent.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentDeleteRequest {
    private String agentId;
    private Long endTime;
}
