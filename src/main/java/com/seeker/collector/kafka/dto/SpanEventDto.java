package com.seeker.collector.kafka.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpanEventDto {
    private String traceId;
    private long spanId;
    private String agentId;
    private String applicationName;
    private int sequence;
    private long startTime;
    private int endElapsed;
    private int depth;
    private int serviceType;
    private String destinationId;
    private long nextSpanId;
    private int apiId;
    private String exceptionInfo;
}
