package com.seeker.collector.grpc;

import com.seeker.collector.global.grpc.CollectResponse;
import com.seeker.collector.global.grpc.DataMessage;
import com.seeker.collector.global.grpc.LogBatch;
import com.seeker.collector.global.grpc.LogMessage;
import com.seeker.collector.global.grpc.TraceId;
import com.seeker.collector.kafka.dto.payload.LogPayload;
import com.seeker.collector.kafka.producer.LogKafkaProducer;
import com.seeker.collector.kafka.producer.MetricKafkaProducer;
import com.seeker.collector.kafka.producer.TraceDataKafkaProducer;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorGrpcServiceTest {

    @Test
    void dispatchesLogBatchToLogKafkaProducer() {
        TraceDataKafkaProducer traceDataKafkaProducer = mock(TraceDataKafkaProducer.class);
        MetricKafkaProducer metricKafkaProducer = mock(MetricKafkaProducer.class);
        LogKafkaProducer logKafkaProducer = mock(LogKafkaProducer.class);
        when(logKafkaProducer.sendLog(any(LogPayload.class))).thenReturn(Mono.empty());

        CollectorGrpcService service = new CollectorGrpcService(
                traceDataKafkaProducer,
                metricKafkaProducer,
                logKafkaProducer
        );

        DataMessage dataMessage = DataMessage.newBuilder()
                .setLogBatch(LogBatch.newBuilder()
                        .addAllLogs(List.of(logMessage("trace-a"), logMessage("trace-b")))
                        .build())
                .build();

        StreamObserver<DataMessage> requestObserver = service.collect(mockResponseObserver());
        requestObserver.onNext(dataMessage);

        ArgumentCaptor<LogPayload> captor = ArgumentCaptor.forClass(LogPayload.class);
        verify(logKafkaProducer, times(2)).sendLog(captor.capture());

        LogPayload firstPayload = captor.getAllValues().get(0);
        assertThat(firstPayload.getTimestamp()).isEqualTo(1000L);
        assertThat(firstPayload.getObservedTimestamp()).isEqualTo(1100L);
        assertThat(firstPayload.getTraceId()).isEqualTo("trace-a");
        assertThat(firstPayload.getSpanId()).isEqualTo(10L);
        assertThat(firstPayload.getParentSpanId()).isEqualTo(9L);
        assertThat(firstPayload.getTraceFlags()).isEqualTo(1);
        assertThat(firstPayload.getAgentId()).isEqualTo("agent-1");
        assertThat(firstPayload.getServiceName()).isEqualTo("checkout");
        assertThat(firstPayload.getAgentGroup()).isEqualTo("prod");
        assertThat(firstPayload.getLoggerName()).isEqualTo("com.seeker.Checkout");
        assertThat(firstPayload.getThreadName()).isEqualTo("http-nio-1");
        assertThat(firstPayload.getSeverityText()).isEqualTo("ERROR");
        assertThat(firstPayload.getSeverityNumber()).isEqualTo(17);
        assertThat(firstPayload.getBody()).isEqualTo("failed to process request");
        assertThat(firstPayload.getAttributes()).containsEntry("orderId", "order-1");
        assertThat(firstPayload.getExceptionType()).isEqualTo("IllegalStateException");
        assertThat(firstPayload.getExceptionMessage()).isEqualTo("invalid state");
        assertThat(firstPayload.getExceptionStacktrace()).isEqualTo("stacktrace");
    }

    private LogMessage logMessage(String traceId) {
        return LogMessage.newBuilder()
                .setTimestamp(1000L)
                .setObservedTimestamp(1100L)
                .setTraceId(TraceId.newBuilder()
                        .setTraceId(traceId)
                        .setSpanId(10L)
                        .setParentSpanId(9L)
                        .setFlags(1)
                        .build())
                .setTraceFlags(1)
                .setAgentId("agent-1")
                .setServiceName("checkout")
                .setAgentGroup("prod")
                .setLoggerName("com.seeker.Checkout")
                .setThreadName("http-nio-1")
                .setSeverityText("ERROR")
                .setSeverityNumber(17)
                .setBody("failed to process request")
                .putAllAttributes(Map.of("orderId", "order-1"))
                .setExceptionType("IllegalStateException")
                .setExceptionMessage("invalid state")
                .setExceptionStacktrace("stacktrace")
                .build();
    }

    @SuppressWarnings("unchecked")
    private StreamObserver<CollectResponse> mockResponseObserver() {
        return mock(StreamObserver.class);
    }
}
