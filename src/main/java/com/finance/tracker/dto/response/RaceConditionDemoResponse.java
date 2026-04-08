package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured response for the race condition demonstration.")
public class RaceConditionDemoResponse {

    private int threadCount;
    private int incrementsPerThread;
    private int expectedValue;
    private CounterResult unsafeCounter;
    private CounterResult atomicCounter;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Aggregated counter result for one concurrency strategy.")
    public static class CounterResult {

        private String name;
        private int actualValue;
        private int lostUpdates;
        private boolean matchesExpected;
        private String verdict;
    }
}
