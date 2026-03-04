package com.seeker.collector.config;

import com.seeker.collector.grpc.CollectorGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    private final CollectorGrpcService collectorGrpcService;

    @Value("${grpc.server.port}")
    private int grpcPort;

    private Server server;

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(grpcPort)
                .addService(collectorGrpcService)
                .build()
                .start();
        log.info("[Collector] gRPC 서버 시작 - port: {}", grpcPort);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("[Collector] gRPC 서버 종료");
        }
    }
}
