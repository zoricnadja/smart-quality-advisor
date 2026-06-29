package com.ftn.sbnz.service.controller;

import com.ftn.sbnz.service.dto.BatchEvaluationRequest;
import com.ftn.sbnz.service.dto.EvaluationResult;
import com.ftn.sbnz.service.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @GetMapping(value = "/demo", produces = MediaType.TEXT_PLAIN_VALUE)
    public String runDemo() {
        return qualityService.runDemo();
    }

    /**
     * GET /api/quality/cep-demo
     * Runs CEP aggregation and chained trend scenarios.
     */
    @GetMapping("/cep-demo")
    public String runCepDemo() {
        return qualityService.runCepDemo();
    }

    /**
     * GET /api/quality/cep-pseudo-clock-demo
     * Runs the CEP smoke-temperature scenario with a pseudo clock.
     */
    @GetMapping("/cep-pseudo-clock-demo")
    public String runCepPseudoClockDemo() {
        return qualityService.runCepPseudoClockDemo();
    }

    /**
     * GET /api/quality/template-demo
     * Runs the Drools template demonstration.
     */
    @GetMapping(value = "/template-demo", produces = MediaType.TEXT_PLAIN_VALUE)
    public String runTemplateDemo() {
        return qualityService.runTemplateDemo();
    }

    /**
     * GET /api/quality/backward-demo
     * Runs a blocked fermentation scenario and returns the recursive backward chaining tree.
     */
    @GetMapping("/backward-demo")
    public String runBackwardChainingDemo() {
        return qualityService.runBackwardChainingDemo();
    }

    /**
     * POST /api/quality/evaluate
     * Interactive evaluation of a single batch with user-supplied parameters
     * (and optional template rules) for the chosen phase. Returns the decision,
     * triggered alerts/warnings, the reasoning log and the backward-chaining
     * explanation when the batch is blocked.
     */
    @PostMapping(value = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public EvaluationResult evaluate(@RequestBody BatchEvaluationRequest request) {
        return qualityService.evaluateBatch(request);
    }
}
