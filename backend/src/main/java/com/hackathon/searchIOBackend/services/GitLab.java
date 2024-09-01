package com.hackathon.searchIOBackend.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class GitLab {

    private static final Logger LOGGER = Logger.getLogger(GitLab.class.getName());
    private static final int MAX_RESULTS = 50;

    @Value("${gitlab.api.url}")
    private String gitlabApiUrl;

    @Value("${gitlab.api.token}")
    private String gitlabApiToken;


    /**
     * Searches GitLab for issues, merge requests, and projects based on the search term.
     * Aggregates the results into a summary and a list of URLs.
     *
     * @param searchTerm The term to search for in GitLab.
     * @return A list containing the summary of results and URLs found.
     * @throws Exception If an error occurs during the search.
     */
    public List<String> searchGitLab(String searchTerm) throws Exception {
        LOGGER.info("Starting GitLab search with term: " + searchTerm);

        List<String> allUrls = new ArrayList<>();
        StringBuilder summaryBuilder = new StringBuilder();

        // Search GitLab resources
        List<GitLabResult> issueResults = searchGitLabResource("issues", searchTerm);
        List<GitLabResult> mergeRequestResults = searchGitLabResource("merge_requests", searchTerm);
        List<GitLabResult> projectResults = searchGitLabResource("projects", searchTerm);

        // Build summary with results and aggregate URLs
        if (!issueResults.isEmpty()) {
            summaryBuilder.append("<h4>Issues: ").append(issueResults.size()).append(" found</h4>\n<ul>\n");
            for (GitLabResult result : issueResults) {
                summaryBuilder.append("<li><a href=\"").append(result.getUrl()).append("\">")
                        .append(result.getTitle()).append("</a></li>\n");
                allUrls.add(result.getUrl());
            }
            summaryBuilder.append("</ul>\n");
        }

        if (!mergeRequestResults.isEmpty()) {
            summaryBuilder.append("<h4>Merge Requests: ").append(mergeRequestResults.size()).append(" found</h4>\n<ul>\n");
            for (GitLabResult result : mergeRequestResults) {
                summaryBuilder.append("<li><a href=\"").append(result.getUrl()).append("\">")
                        .append(result.getTitle()).append("</a></li>\n");
                allUrls.add(result.getUrl());
            }
            summaryBuilder.append("</ul>\n");
        }

        if (!projectResults.isEmpty()) {
            summaryBuilder.append("<h4>Projects: ").append(projectResults.size()).append(" found</h4>\n<ul>\n");
            for (GitLabResult result : projectResults) {
                summaryBuilder.append("<li><a href=\"").append(result.getUrl()).append("\">")
                        .append(result.getTitle()).append("</a></li>\n");
                allUrls.add(result.getUrl());
            }
            summaryBuilder.append("</ul>\n");
        }

        // Log summary and results
        LOGGER.info("GitLab search completed. Summary: " + summaryBuilder.toString());
        LOGGER.info("Total URLs found: " + allUrls.size());

        if (issueResults.isEmpty() && projectResults.isEmpty() && mergeRequestResults.isEmpty()) {
            LOGGER.info("No results found for the search term: " + searchTerm);
            return allUrls;
        }

        // Return combined summary and URLs
        return List.of(summaryBuilder.toString(), String.join(", ", allUrls));
    }

    /**
     * Searches a specific GitLab resource (issues, merge requests, projects) for the given search term.
     *
     * @param endpoint The GitLab endpoint to search (e.g., "issues", "merge_requests", "projects").
     * @param searchTerm The term to search for in the specified GitLab resource.
     * @return A list of GitLab results.
     * @throws Exception If an error occurs during the search.
     */
    private List<GitLabResult> searchGitLabResource(String endpoint, String searchTerm) {
        List<GitLabResult> results = new ArrayList<>();
        LOGGER.info("Searching GitLab resource: " + endpoint + " with term: " + searchTerm);

        HttpURLConnection conn = null;
        BufferedReader in = null;

        try {
            // Encode the search term
            String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
            String apiUrl = String.format("%s/%s?search=%s", gitlabApiUrl, endpoint, encodedSearchTerm);
            LOGGER.info("Constructed GitLab API URL: " + apiUrl);

            // Open connection and set request properties
            conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("PRIVATE-TOKEN", gitlabApiToken);

            int responseCode = conn.getResponseCode();
            LOGGER.info("Response code from GitLab API for endpoint " + endpoint + ": " + responseCode);

            // Handle non-OK response codes
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warning("Received non-OK response code: " + responseCode);
                return results; // Return empty list for non-critical issues
            }

            // Read response
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            // Parse JSON response
            JSONArray items = new JSONArray(response.toString());
            LOGGER.info("Number of items received from GitLab API for endpoint " + endpoint + ": " + items.length());

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String url = item.optString("web_url"); // Adjust if necessary
                String title = null;

                // Extract title based on the endpoint
                switch (endpoint) {
                    case "issues":
                        title = item.optString("title");
                        break;
                    case "projects":
                        title = item.optString("name");
                        break;
                    default:
                        LOGGER.warning("Unknown endpoint: " + endpoint);
                }

                if (url != null && !url.isEmpty()) {
                    results.add(new GitLabResult(title, url));
                }

                // Limit results to MAX_RESULTS
                if (results.size() >= MAX_RESULTS) {
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.severe("I/O error occurred while searching GitLab resource " + endpoint + ": " + e.getMessage());
            // Handle I/O errors gracefully, don't throw exception
        } catch (Exception e) {
            LOGGER.severe("Unexpected error occurred while searching GitLab resource " + endpoint + ": " + e.getMessage());
            // Handle unexpected errors gracefully, don't throw exception
        } finally {
            // Ensure resources are closed
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.warning("Failed to close BufferedReader: " + e.getMessage());
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Log the number of results found
        LOGGER.info("Number of results found for " + endpoint + ": " + results.size());
        return results;
    }


    /**
     * A class representing the result of a GitLab search.
     */
    private static class GitLabResult {
        private final String title;
        private final String url;

        public GitLabResult(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}
