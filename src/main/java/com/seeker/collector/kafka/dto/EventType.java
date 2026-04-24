package com.seeker.collector.kafka.dto;

public enum EventType {

    // agent 관련 enum
    AGENT_CREATED,
    AGENT_DELETED,

    // trace data 관련 enum
    TRACE,
    SPAN,
    SPAN_EVENT

}
