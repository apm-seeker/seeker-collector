package com.seeker.collector.agent.controller;

import com.seeker.collector.agent.dto.AgentDto;
import com.seeker.collector.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createAgent(
            @RequestBody AgentDto agentDto
        ) {

        return agentService.createAgent(agentDto)
                .map(result -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()));
    }

    @DeleteMapping("/{agentId}")
    public Mono<ResponseEntity<Void>> deleteAgent(
            @PathVariable UUID agentId
    ) {

        return agentService.deleteAgent(agentId)
                .map(result -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()));
    }
}
