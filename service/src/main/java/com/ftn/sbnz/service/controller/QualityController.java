package com.ftn.sbnz.service.controller;

import com.ftn.sbnz.service.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    @Autowired
    private QualityService qualityService;

    /**
     * GET /api/quality/demo
     * Runs the full demonstration for all scenarios.
     */
    @GetMapping("/demo")
    public String runDemo() {
        qualityService.runDemo();
        return "Demonstration finished. Check the console for results.";
    }
}