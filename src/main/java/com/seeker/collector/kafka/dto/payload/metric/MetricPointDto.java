package com.seeker.collector.kafka.dto.payload.metric;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class MetricPointDto {

    private String metricName;
    private String fieldName;
    private double value;
    private MetricValueType type;
    private Map<String, String> tags;

}
