package com.seeker.collector.agent.service;

import com.seeker.collector.agent.dto.AgentCreateRequest;
import com.seeker.collector.agent.dto.AgentDeleteRequest;
import com.seeker.collector.kafka.dto.payload.AgentCreatedPayload;
import com.seeker.collector.kafka.dto.payload.AgentDeletedPayload;
import com.seeker.collector.kafka.producer.AgentKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentKafkaProducer agentKafkaProducer;

    public Mono<Void> createAgent(AgentCreateRequest agentRequest) {
        AgentCreatedPayload payload = AgentCreatedPayload
                .builder()
                .agentId(agentRequest.getAgentId())
                .agentName(agentRequest.getAgentName())
                .agentType(agentRequest.getAgentType())
                .agentGroup(agentRequest.getAgentGroup())
                .startTime(agentRequest.getStartTime())
                .build();

        return agentKafkaProducer.sendCreatedEvent(payload);
    }

    public Mono<Void> deleteAgent(AgentDeleteRequest agentRequest) {
        AgentDeletedPayload payload = AgentDeletedPayload
                .builder()
                .agentId(agentRequest.getAgentId())
                .endTime(agentRequest.getEndTime())
                .build();
        return agentKafkaProducer.sendDeletedEvent(payload);
    }
}
