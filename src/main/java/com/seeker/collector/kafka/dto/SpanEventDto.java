package com.seeker.collector.kafka.dto;

import java.util.Map;
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
    private int elapsedTime;
    private int depth;
    private int methodType;
    private Map<String,String> attributes;
    private String exceptionInfo;
}
