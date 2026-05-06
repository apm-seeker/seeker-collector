package com.seeker.collector.config;

import com.seeker.collector.grpc.CollectorGrpcService;
import com.seeker.collector.otlp.OtlpTraceService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * 단일 gRPC 서버를 띄우고 두 개의 서비스를 함께 등록.
 *
 * <p>한 포트에 여러 서비스를 올려도 충돌하지 않는 이유:
 * gRPC 는 HTTP/2 의 :path 헤더 {@code /<package.Service>/<Method>} 로 라우팅하므로
 * 서비스 풀네임만 달라도 분기됨.
 *
 * <ul>
 *   <li>{@code com.seeker.collector.global.grpc.CollectorService/collect}
 *       → {@link CollectorGrpcService} (자체 protocol, client-streaming)</li>
 *   <li>{@code opentelemetry.proto.collector.trace.v1.TraceService/Export}
 *       → {@link OtlpTraceService} (OTLP 표준, unary)</li>
 * </ul>
 *
 * <p>등록 검증 방법: <code>grpcurl -plaintext localhost:9999 list</code>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    // 자체 protocol (Java agent 가 사용)
    private final CollectorGrpcService collectorGrpcService;

    // OTLP/gRPC (Envoy 등 표준 OTel 클라이언트가 사용)
    private final OtlpTraceService otlpTraceService;

    @Value("${grpc.server.port}")
    private int grpcPort;

    private Server server;

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(grpcPort)
                // 두 서비스를 같은 포트에 등록 — :path 로 라우팅되므로 충돌 없음
                .addService(collectorGrpcService)
                .addService(otlpTraceService)
                .build()
                .start();
        log.info("[Collector] gRPC 서버 시작 - port: {}", grpcPort);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            // graceful shutdown — 진행 중인 RPC 가 끝날 때까지 대기 후 종료
            server.shutdown();
            log.info("[Collector] gRPC 서버 종료");
        }
    }
}
