package com.seeker.collector.kafka.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpanDto {
    private String traceId;
    private long spanId;
    private long parentSpanId;
    private String agentId;
    private String applicationName;
    private long startTime;
    private int elapsedTime;
    private String uri;
    private String remoteAddr;
    private String endPoint;
    private int serviceType;
    private String exceptionInfo;
}
