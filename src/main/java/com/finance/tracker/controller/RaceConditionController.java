package com.finance.tracker.controller;

import com.finance.tracker.service.impl.RaceConditionDemoService;
import java.util.HashMap;
import java.util.Map;
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
    public ResponseEntity<Map<String, String>> runRaceConditionDemo() throws InterruptedException {
        demoService.runAllDemos();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Race condition demonstration started");
        response.put("status", "Check logs for results");

        return ResponseEntity.ok(response);
    }
}
