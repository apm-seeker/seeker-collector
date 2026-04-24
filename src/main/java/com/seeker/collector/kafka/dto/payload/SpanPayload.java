package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpanPayload implements TraceDataPayload {
    private long spanId;
    private long parentSpanId;
    private String traceId;
    private String agentId;
    private long startTime;
    private int elapsedTime;
    private String uri;
    private String remoteAddress;
    private String endPoint;
    private int statusCode;
    private String exceptionInfo;
}
