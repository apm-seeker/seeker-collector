package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpanEventPayload implements TraceDataPayload {
    private int sequence;
    private int depth;
    private long startTime;
    private int elapsedTime;
    private String className;
    private String methodName;
    private String exceptionInfo;
}
