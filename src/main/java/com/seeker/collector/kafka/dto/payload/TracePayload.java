package com.seeker.collector.kafka.dto.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TracePayload implements TraceDataPayload {
    private String traceId;
    private Long startTime;
    private int elapsedTime;
    private String url;
    private String remoteAddress;
    private int statusCode;
}
