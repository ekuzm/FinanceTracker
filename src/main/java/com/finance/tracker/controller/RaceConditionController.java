package com.finance.tracker.controller;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import com.finance.tracker.service.impl.RaceConditionDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/race-condition")
@RequiredArgsConstructor
public class RaceConditionController {

    private final RaceConditionDemoService demoService;

    @GetMapping("/run")
    public ResponseEntity<RaceConditionDemoResponse> runRaceConditionDemo() throws InterruptedException {
        return ResponseEntity.ok(demoService.runAllDemos());
    }
}
