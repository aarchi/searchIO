package com.hackathon.searchIOBackend.model;

import java.util.List;

public class AggregatedSearchResponse {

    private List<AggregatedResult> results;

    public AggregatedSearchResponse() {}

    public AggregatedSearchResponse(List<AggregatedResult> results) {
        this.results = results;
    }

    public List<AggregatedResult> getResults() {
        return results;
    }

    public void setResults(List<AggregatedResult> results) {
        this.results = results;
    }

    public static class AggregatedResult {
        private String title;
        private String summary;
        private List<String> urls;

        public AggregatedResult(String title, String summary, List<String> urls) {
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
}
