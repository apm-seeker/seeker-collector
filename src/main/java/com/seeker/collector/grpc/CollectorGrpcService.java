package com.seeker.collector.grpc;

import com.seeker.collector.global.grpc.*;
import com.seeker.collector.kafka.dto.payload.SpanEventPayload;
import com.seeker.collector.kafka.dto.payload.SpanPayload;
import com.seeker.collector.kafka.dto.payload.TracePayload;
import com.seeker.collector.kafka.producer.TraceDataKafkaProducer;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcService extends CollectorServiceGrpc.CollectorServiceImplBase {

    private final TraceDataKafkaProducer traceDataKafkaProducer;

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
                log.error("[Collector] gRPC 스트림 에러 - 수신 메시지 수: {}", receivedCount, t);
                responseObserver.onError(Status.fromThrowable(t).asRuntimeException());
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

        SpanPayload spanPayload = toSpanPayload(span);
        String traceId = spanPayload.getTraceId();

        traceDataKafkaProducer.sendSpan(spanPayload, traceId)
                .subscribe(null, err -> {});

        if (spanPayload.getParentSpanId() == -1) {
            traceDataKafkaProducer.sendTrace(toTracePayload(span), traceId)
                    .subscribe(null, err -> {});
        }

        for (SpanEvent spanEvent : span.getSpanEventListList()) {
            SpanEventPayload spanEventPayload = toSpanEventPayload(spanEvent);

            traceDataKafkaProducer.sendSpanEvent(spanEventPayload, traceId)
                    .subscribe(null, err -> {});
        }
    }

    private TracePayload toTracePayload(Span span) {
        return TracePayload
                .builder()
                .traceId(span.getTraceId().getTraceId())
                .startTime(span.getStartTime())
                .elapsedTime(span.getElapsedTime())
                .url(span.getUri())
                .remoteAddress(span.getRemoteAddr())
                .statusCode(span.getStatusCode())
                .build();
    }

    private SpanPayload toSpanPayload(Span span) {
        return SpanPayload
                .builder()
                .spanId(span.getTraceId().getSpanId())
                .parentSpanId(span.getTraceId().getParentSpanId())
                .traceId(span.getTraceId().getTraceId())
                .agentId(span.getAgentId())
                .startTime(span.getStartTime())
                .elapsedTime(span.getElapsedTime())
                .uri(span.getUri())
                .remoteAddress(span.getRemoteAddr())
                .endPoint(span.getEndPoint())
                .statusCode(span.getStatusCode())
                .exceptionInfo(span.getExceptionInfo())
                .build();
    }

    private SpanEventPayload toSpanEventPayload(SpanEvent spanEvent) {
        return SpanEventPayload
                .builder()
                .sequence(spanEvent.getSequence())
                .depth(spanEvent.getDepth())
                .startTime(spanEvent.getStartTime())
                .elapsedTime(spanEvent.getElapsedTime())
                .className(spanEvent.getAttributesOrDefault("className", null))
                .methodName(spanEvent.getAttributesOrDefault("methodName", null))
                .exceptionInfo(spanEvent.getExceptionInfo())
                .build();
    }

}
