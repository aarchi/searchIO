package com.hackathon.searchIOBackend.controllers;

public class AdminRequest {
    private String incident;
    private String impact;
    private String resolution;

    // Getters and setters
    public String getIncident() { return incident; }
    public void setIncident(String incident) { this.incident = incident; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
