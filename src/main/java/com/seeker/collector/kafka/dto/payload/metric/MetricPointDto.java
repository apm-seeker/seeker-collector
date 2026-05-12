package com.seeker.collector.kafka.dto.payload.metric;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetricPointDto {

    private String metricName;
    private String fieldName;
    private double value;
    MetricValueType type;

}
