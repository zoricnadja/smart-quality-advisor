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
     * Pokreće kompletnu demonstraciju svih scenarija.
     */
    @GetMapping("/demo")
    public String pokreniDemo() {
        qualityService.pokreniDemonstraciju();
        return "Demonstracija završena. Pogledajte konzolu za rezultate.";
    }
}
