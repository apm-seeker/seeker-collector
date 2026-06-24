package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class LogPayload {

    private long timestamp;
    private long observedTimestamp;

    private String traceId;
    private long spanId;
    private long parentSpanId;
    private int traceFlags;

    private String agentId;
    private String serviceName;
    private String agentGroup;

    private String loggerName;
    private String threadName;
    private String severityText;
    private int severityNumber;
    private String body;

    private Map<String, String> attributes;

    private String exceptionType;
    private String exceptionMessage;
    private String exceptionStacktrace;
}
