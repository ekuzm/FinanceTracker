package com.finance.tracker.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AsyncTask {

    private String taskId;
    private AsyncTaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer progress;
    private String result;
}
