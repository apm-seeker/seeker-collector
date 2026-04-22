package com.seeker.collector.grpc;

import com.seeker.collector.global.grpc.*;
import com.seeker.collector.kafka.dto.SpanDto;
import com.seeker.collector.kafka.dto.SpanEventDto;
import com.seeker.collector.kafka.producer.SpanKafkaProducer;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcService extends CollectorServiceGrpc.CollectorServiceImplBase {

    private final SpanKafkaProducer spanKafkaProducer;

    @Override
    public StreamObserver<DataMessage> collect(StreamObserver<CollectResponse> responseObserver) {
        return new StreamObserver<>() {
            private int receivedCount = 0;

            @Override
            public void onNext(DataMessage dataMessage) {
                receivedCount++;

                if (dataMessage.hasSpan()) {
                    handleSpan(dataMessage.getSpan());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[Collector] gRPC 스트림 에러", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(CollectResponse.newBuilder()
                        .setSuccess(true)
                        .setReceivedCount(receivedCount)
                        .build());
                responseObserver.onCompleted();
                log.info("[Collector] 스트림 완료 - 수신 메시지 수: {}", receivedCount);
            }
        };
    }

    private void handleSpan(Span span) {
        TraceId traceId = span.getTraceId();

        SpanDto spanDto = SpanDto.builder()
                .traceId(traceId.getTraceId())
                .spanId(traceId.getSpanId())
                .parentSpanId(traceId.getParentSpanId())
                .agentId(span.getAgentId())
                .applicationName(span.getApplicationName())
                .startTime(span.getStartTime())
                .elapsedTime(span.getElapsedTime())
                .uri(span.getUri())
                .remoteAddr(span.getRemoteAddr())
                .endPoint(span.getEndPoint())
                .serviceType(span.getServiceType())
                .exceptionInfo(span.getExceptionInfo())
                .build();

        spanKafkaProducer.sendSpan(spanDto);

        for (SpanEvent event : span.getSpanEventListList()) {
            SpanEventDto eventDto = SpanEventDto.builder()
                    .traceId(traceId.getTraceId())
                    .spanId(traceId.getSpanId())
                    .agentId(span.getAgentId())
                    .applicationName(span.getApplicationName())
                    .sequence(event.getSequence())
                    .startTime(event.getStartTime())
                    .elapsedTime(event.getElapsedTime())
                    .depth(event.getDepth())
                    .methodType(event.getMethodType())
                    .attributes(event.getAttributesMap())
                    .exceptionInfo(event.getExceptionInfo())
                    .build();

            spanKafkaProducer.sendSpanEvent(eventDto);
        }

        log.debug("[Collector] Span 처리 완료 - traceId: {}, spanEvents: {}",
                traceId.getTraceId(), span.getSpanEventListCount());
    }
}
