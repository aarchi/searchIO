package com.hackathon.searchIOBackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${api.url}")
    private String apiUrl;

    @GetMapping
    public String getApiUrl() {
        return apiUrl;
    }
}
