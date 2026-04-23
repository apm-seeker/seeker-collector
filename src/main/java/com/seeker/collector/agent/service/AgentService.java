package com.seeker.collector.agent.service;

import com.seeker.collector.agent.dto.AgentDto;
import com.seeker.collector.kafka.dto.AgentEventDto;
import com.seeker.collector.kafka.dto.AgentEventType;
import com.seeker.collector.kafka.producer.AgentKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentKafkaProducer agentKafkaProducer;

    public Mono<Void> createAgent(AgentDto agentDto) {
        AgentEventDto agentEventDto = AgentEventDto
                .builder()
                .agentEventType(AgentEventType.AGENT_CREATED)
                .agentId(agentDto.getAgentId())
                .payload(agentDto)
                .build();
        return agentKafkaProducer.sendAgent(agentEventDto);
    }

    public Mono<Void> deleteAgent(UUID agentId) {
        AgentEventDto agentEventDto = AgentEventDto
                .builder()
                .agentEventType(AgentEventType.AGENT_DELETED)
                .agentId(agentId)
                .build();
        return agentKafkaProducer.sendAgent(agentEventDto);
    }

}
