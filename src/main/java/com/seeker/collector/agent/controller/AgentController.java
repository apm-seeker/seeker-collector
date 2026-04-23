package com.seeker.collector.agent.controller;

import com.seeker.collector.agent.dto.AgentCreateRequest;
import com.seeker.collector.agent.dto.AgentDeleteRequest;
import com.seeker.collector.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createAgent(
            @RequestBody AgentCreateRequest agentRequest
        ) {

        return agentService.createAgent(agentRequest)
                .map(result -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()));
    }

    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteAgent(
            @RequestBody AgentDeleteRequest agentRequest
    ) {

        return agentService.deleteAgent(agentRequest)
                .map(result -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()));
    }
}
