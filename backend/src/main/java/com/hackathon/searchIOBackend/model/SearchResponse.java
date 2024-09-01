package com.hackathon.searchIOBackend.model;

import java.util.List;

public class SearchResponse {

    private String title;
    private String summary;
    private List<String> urls;

    public SearchResponse() {}

    public SearchResponse(String title, String summary, List<String> urls) {
        this.title = title;
        this.summary = summary;
        this.urls = urls;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
