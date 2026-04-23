package com.seeker.collector.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AgentDto {
    private UUID agentId;
    private String agentName;
    private String agentType;
    private String agentGroup;
    private Long startTime;
}
