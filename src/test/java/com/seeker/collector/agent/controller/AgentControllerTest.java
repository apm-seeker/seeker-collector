package com.seeker.collector.agent.controller;

import com.seeker.collector.agent.dto.AgentDto;
import com.seeker.collector.agent.service.AgentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebFluxTest(controllers = AgentController.class)
class AgentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AgentService agentService;

    @Test
    @DisplayName("POST /agents - 카프카 발행 성공 시 200 OK")
    void createAgent_success_returns200() {
        AgentDto request = AgentDto.builder()
                .agentId(UUID.randomUUID())
                .agentName("test-agent")
                .agentType("JAVA")
                .agentGroup("default")
                .startTime(System.currentTimeMillis())
                .build();

        given(agentService.createAgent(any(AgentDto.class)))
                .willReturn(Mono.empty());

        webTestClient.post()
                .uri("/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(agentService).createAgent(any(AgentDto.class));
    }

    @Test
    @DisplayName("POST /agents - 카프카 발행 실패 시 503 SERVICE_UNAVAILABLE")
    void createAgent_kafkaFailure_returns503() {
        AgentDto request = AgentDto.builder()
                .agentId(UUID.randomUUID())
                .agentName("test-agent")
                .agentType("JAVA")
                .agentGroup("default")
                .startTime(System.currentTimeMillis())
                .build();

        given(agentService.createAgent(any(AgentDto.class)))
                .willReturn(Mono.error(new RuntimeException("kafka down")));

        webTestClient.post()
                .uri("/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("DELETE /agents/{agentId} - 카프카 발행 성공 시 200 OK")
    void deleteAgent_success_returns200() {
        UUID agentId = UUID.randomUUID();

        given(agentService.deleteAgent(agentId))
                .willReturn(Mono.empty());

        webTestClient.delete()
                .uri("/agents/{agentId}", agentId)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(agentService).deleteAgent(agentId);
    }

    @Test
    @DisplayName("DELETE /agents/{agentId} - 카프카 발행 실패 시 503 SERVICE_UNAVAILABLE")
    void deleteAgent_kafkaFailure_returns503() {
        UUID agentId = UUID.randomUUID();

        given(agentService.deleteAgent(agentId))
                .willReturn(Mono.error(new RuntimeException("kafka down")));

        webTestClient.delete()
                .uri("/agents/{agentId}", agentId)
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("DELETE /agents/{agentId} - UUID 형식이 아니면 400 BAD_REQUEST")
    void deleteAgent_invalidUuid_returns400() {
        webTestClient.delete()
                .uri("/agents/not-a-uuid")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
