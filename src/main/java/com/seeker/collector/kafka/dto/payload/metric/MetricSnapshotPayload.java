package com.seeker.collector.kafka.dto.payload.metric;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetricSnapshotPayload {

    private String applicationName;
    private String agentId;
    private Long timestamp;
    private Long collectIntervalMs;
    private List<MetricPointDto> points;

}
