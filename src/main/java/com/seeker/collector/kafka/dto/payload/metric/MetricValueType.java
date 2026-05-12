package com.seeker.collector.kafka.dto.payload.metric;

public enum MetricValueType {

    GAUGE,      // 폴링 시점의 현재값 (heap_used, threadCount 등)
    CUMULATIVE, // 시작 이후 누적값. 서버에서 인접 두 시점 차분 (gc.count, classes_loaded)
    DELTA,      // 에이전트가 이미 직전 cycle 대비 차분으로 변환해 보낸 값 (transaction)
    WINDOW,     // 한 cycle 동안의 윈도우값. 다음 cycle에서 reset됨 (response_time avg/max)

}
