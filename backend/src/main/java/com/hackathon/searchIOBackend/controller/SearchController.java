package com.hackathon.searchIOBackend.controller;

import com.hackathon.searchIOBackend.model.AggregatedSearchResponse;
import com.hackathon.searchIOBackend.model.FormData;
import com.hackathon.searchIOBackend.model.SearchRequest;
import com.hackathon.searchIOBackend.model.SearchResponse;
import com.hackathon.searchIOBackend.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")  // Enable CORS for Angular frontend
public class SearchController {

    private static final Logger LOGGER = Logger.getLogger(SearchController.class.getName());

    @Autowired
    private StackOverflow stackOverflowService;

    @Autowired
    private GitLab gitLabService;

    @Autowired
    private UBSAssist ubsAssistService;

    @Autowired
    private Confluence confluenceService;

    @Autowired
    private Incidents incidentService;

    @Autowired
    private AdminService adminService;

    @PostMapping("/stackOverflow-search")
    public ResponseEntity<SearchResponse> searchStackOverflow(@RequestBody SearchRequest request) {
        LOGGER.info("Received Stack Overflow search request with search term: " + request.getSearchTerm());

        String searchTerm = request.getSearchTerm();
        List<String> links;

        try {
            links = stackOverflowService.searchStackOverflow(searchTerm);
        } catch (Exception e) {
            String errorMessage = "There is an issue with the Stack Overflow API call. Please try again later.";
            LOGGER.severe(errorMessage + " Error details: " + e.getMessage());

            SearchResponse response = new SearchResponse("STACKOVERFLOW",errorMessage, new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        LOGGER.info("Retrieved question URLs: " + links);

        if (links.isEmpty()) {
            String noResultsMessage = "No relevant results found on Stack Overflow for the search term: " + searchTerm;
            LOGGER.info(noResultsMessage);

            SearchResponse response = new SearchResponse("StackOverflow",noResultsMessage, new ArrayList<>());
            return ResponseEntity.ok(response);
        }

        String summaryOrErrorMessage = stackOverflowService.fetchAndSummarizeBodies(links, searchTerm);
        LOGGER.info("Summary or error message: " + summaryOrErrorMessage);

        SearchResponse response = new SearchResponse("StackOverflow",summaryOrErrorMessage, links);
        return ResponseEntity.ok(response);
    }


    @Value("${pdf.storage.dir:pdf-storage}")
    private String pdfStorageDir;


    @PostMapping("/admin")
    public ResponseEntity<Map<String, String>> handleFormData(@RequestBody FormData formData) {
        try {
            // Append data to JSON file
            adminService.appendDataToJsonFile(formData);

            // Optionally, send the JSON file to an API or perform additional actions
            // adminService.sendPdfToApi(jsonFileName); // If needed

            // Return success response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Data processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log and return an error response
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error appending data to PDF");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/confluence-search")
    public ResponseEntity<SearchResponse> searchConfluence(@RequestBody SearchRequest request) {
        LOGGER.info("Received Confluence search request with search term: " + request.getSearchTerm());

        String searchTerm = request.getSearchTerm();
        List<String> links;

        try {
            links = confluenceService.searchConcluence(searchTerm);
        } catch (Exception e) {
            String errorMessage = "There is an issue with the Confluence API call. Please try again later.";
            LOGGER.severe(errorMessage + " Error details: " + e.getMessage());

            SearchResponse response = new SearchResponse("Confluence",errorMessage, new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        LOGGER.info("Retrieved question URLs: " + links);

        if (links.isEmpty()) {
            String noResultsMessage = "No relevant results found on Confluence for the search term: " + searchTerm;
            LOGGER.info(noResultsMessage);

            SearchResponse response = new SearchResponse("Confluence",noResultsMessage, new ArrayList<>());
            return ResponseEntity.ok(response);
        }

        String summaryOrErrorMessage = confluenceService.fetchAndSummarizeBodies(links, searchTerm);
        LOGGER.info("Summary or error message: " + summaryOrErrorMessage);

        SearchResponse response = new SearchResponse("Confluence",summaryOrErrorMessage, links);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/gitlab-search")
    public ResponseEntity<SearchResponse> searchGitLab(@RequestBody SearchRequest request) {
        LOGGER.info("Received GitLab search request with search term: " + request.getSearchTerm());
        String searchTerm = request.getSearchTerm();
        List<String> searchResults;

        try {
            searchResults = gitLabService.searchGitLab(searchTerm);
        } catch (Exception e) {
            String errorMessage = "There is an issue with the GitLab API call. Please try again later.";
            LOGGER.severe(errorMessage + " Error details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SearchResponse("GitLab",errorMessage, List.of()));
        }

        if (searchResults.isEmpty()) {
            String noResultsMessage = "No relevant results found on GitLab for the search term: " + searchTerm;
            LOGGER.info(noResultsMessage);

            SearchResponse response = new SearchResponse("GitLab",noResultsMessage, new ArrayList<>());
            return ResponseEntity.ok(response);
        }

        LOGGER.info("Retrieved search results");

        String summary = searchResults.get(0);
        List<String> urls = List.of(searchResults.get(1).split(", "));

        SearchResponse response = new SearchResponse("GitLab",summary, new ArrayList<>());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assist-search")
    public ResponseEntity<SearchResponse> searchAssist(@RequestBody SearchRequest request) {
        LOGGER.info("Received UBS Assist search request with search term: " + request.getSearchTerm());

        String searchTerm = request.getSearchTerm();
        Map<String, String> urlSummaryMap;
        String summaryOrErrorMessage;

        try {
            urlSummaryMap = ubsAssistService.searchUBSAssist(searchTerm);

            if (urlSummaryMap.isEmpty()) {
                summaryOrErrorMessage = "No relevant results found on UBS Assist for the search term: " + searchTerm;
            } else {
                StringBuilder summaryBuilder = new StringBuilder();
                summaryBuilder.append("Search completed successfully. Here are the summaries:\n");
                urlSummaryMap.forEach((url, summary) -> summaryBuilder.append(summary).append("\n").append(url).append("\n\n"));

                summaryOrErrorMessage = summaryBuilder.toString().trim();
            }

        } catch (Exception e) {
            summaryOrErrorMessage = "There is an issue with the UBS Assist API call. Please try again later.";
            LOGGER.severe(summaryOrErrorMessage + " Error details: " + e.getMessage());

            SearchResponse response = new SearchResponse("Assist",summaryOrErrorMessage, new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        LOGGER.info("Retrieved URLs: " + urlSummaryMap.keySet());

        SearchResponse response = new SearchResponse("Assist",summaryOrErrorMessage, new ArrayList<>(urlSummaryMap.keySet()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/incident-search")
    public ResponseEntity<SearchResponse> searchIncident(@RequestBody SearchRequest request) {
        LOGGER.info("Received Incident search request with search term: " + request.getSearchTerm());
        String searchTerm = request.getSearchTerm();
        List<String> searchResults;

        try {
            searchResults = incidentService.searchGitLab(searchTerm);
        } catch (Exception e) {
            String errorMessage = "There is an issue with the TocToDocs API call. Please try again later.";
            LOGGER.severe(errorMessage + " Error details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SearchResponse("Incident",errorMessage, List.of()));
        }

        if (searchResults.isEmpty()) {
            String noResultsMessage = "No relevant results found on Incident for the search term: " + searchTerm;
            LOGGER.info(noResultsMessage);

            SearchResponse response = new SearchResponse("Incident",noResultsMessage, new ArrayList<>());
            return ResponseEntity.ok(response);
        }

        LOGGER.info("Retrieved search results");

        String summary = searchResults.get(0);

        SearchResponse response = new SearchResponse("Incident",summary, new ArrayList<>());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search-all")
    public ResponseEntity<AggregatedSearchResponse> searchAll(@RequestBody SearchRequest request) {
        LOGGER.info("Received search-all request with search term: " + request.getSearchTerm());

        String searchTerm = request.getSearchTerm();

        // Define CompletableFutures for each search
        CompletableFuture<SearchResponse> stackOverflowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return searchStackOverflowInternal(searchTerm);
            } catch (Exception e) {
                LOGGER.severe("Error searching StackOverflow: " + e.getMessage());
                return new SearchResponse(); // return an empty or error response
            }
        });

        CompletableFuture<SearchResponse> confluenceFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return searchConfluenceInternal(searchTerm);
            } catch (Exception e) {
                LOGGER.severe("Error searching Confluence: " + e.getMessage());
                return new SearchResponse(); // return an empty or error response
            }
        });

        CompletableFuture<SearchResponse> gitLabFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return searchGitLabInternal(searchTerm);
            } catch (Exception e) {
                LOGGER.severe("Error searching GitLab: " + e.getMessage());
                return new SearchResponse(); // return an empty or error response
            }
        });

        CompletableFuture<SearchResponse> ubsAssistFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return searchAssistInternal(searchTerm);
            } catch (Exception e) {
                LOGGER.severe("Error searching UBS Assist: " + e.getMessage());
                return new SearchResponse(); // return an empty or error response
            }
        });

        CompletableFuture<SearchResponse> incidentFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return searchIncidentInternal(searchTerm);
            } catch (Exception e) {
                LOGGER.severe("Error searching Incident: " + e.getMessage());
                return new SearchResponse(); // return an empty or error response
            }
        });

        // Wait for all futures to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
               stackOverflowFuture, confluenceFuture, gitLabFuture, ubsAssistFuture, incidentFuture
        );

        try {
            allOf.join(); // Ensure all futures are completed
        } catch (Exception e) {
            LOGGER.severe("Error waiting for search results: " + e.getMessage());
            // Return an error response if joining the futures fails
            String noResultsMessage = "Error occurred while fetching search results.";
            List<AggregatedSearchResponse.AggregatedResult> emptyResults = List.of(
                    new AggregatedSearchResponse.AggregatedResult("Error", noResultsMessage, new ArrayList<>())
            );
            AggregatedSearchResponse response = new AggregatedSearchResponse(emptyResults);
            return ResponseEntity.ok(response);
        }

        try {
            List<AggregatedSearchResponse.AggregatedResult> results = new ArrayList<>();

            // Collect results from each future
            results.add(new AggregatedSearchResponse.AggregatedResult("StackOverflow",
                    stackOverflowFuture.get().getSummary(), stackOverflowFuture.get().getUrls()));

            results.add(new AggregatedSearchResponse.AggregatedResult("Confluence",
                    confluenceFuture.get().getSummary(), confluenceFuture.get().getUrls()));

            results.add(new AggregatedSearchResponse.AggregatedResult("GitLab",
                    gitLabFuture.get().getSummary(), gitLabFuture.get().getUrls()));

            results.add(new AggregatedSearchResponse.AggregatedResult("Assist",
                    ubsAssistFuture.get().getSummary(), ubsAssistFuture.get().getUrls()));

            results.add(new AggregatedSearchResponse.AggregatedResult("Incident",
                    incidentFuture.get().getSummary(), incidentFuture.get().getUrls()));

            AggregatedSearchResponse finalResponse = new AggregatedSearchResponse(results);
            return ResponseEntity.ok(finalResponse);

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.severe("Error aggregating search results: " + e.getMessage());
            String noResultsMessage = "No relevant results found for the search term: " + searchTerm;
            List<AggregatedSearchResponse.AggregatedResult> emptyResults = List.of(
                    new AggregatedSearchResponse.AggregatedResult("Error", noResultsMessage, new ArrayList<>())
            );
            AggregatedSearchResponse response = new AggregatedSearchResponse(emptyResults);
            return ResponseEntity.ok(response);
        }
    }


    private SearchResponse searchStackOverflowInternal(String searchTerm) {
        try {
            SearchRequest request = new SearchRequest();
            request.setSearchTerm(searchTerm);
            return searchStackOverflow(request).getBody();
        } catch (Exception e) {
            return new SearchResponse("StackOverflow","Error searching Stack Overflow", new ArrayList<>());
        }
    }

    private SearchResponse searchConfluenceInternal(String searchTerm) {
        try {
            SearchRequest request = new SearchRequest();
            request.setSearchTerm(searchTerm);
            return searchConfluence(request).getBody();
        } catch (Exception e) {
            return new SearchResponse("Confluence","Error searching Confluence", new ArrayList<>());
        }
    }

    private SearchResponse searchGitLabInternal(String searchTerm) {
        try {
            SearchRequest request = new SearchRequest();
            request.setSearchTerm(searchTerm);
            return searchGitLab(request).getBody();
        } catch (Exception e) {
            return new SearchResponse("Gitlab","Error searching GitLab", new ArrayList<>());
        }
    }

    private SearchResponse searchAssistInternal(String searchTerm) {
        try {
            SearchRequest request = new SearchRequest();
            request.setSearchTerm(searchTerm);
            return searchAssist(request).getBody();
        } catch (Exception e) {
            return new SearchResponse("Assist","Error searching UBS Assist", new ArrayList<>());
        }
    }

    private SearchResponse searchIncidentInternal(String searchTerm) {
        try {
            SearchRequest request = new SearchRequest();
            request.setSearchTerm(searchTerm);
            return searchIncident(request).getBody();
        } catch (Exception e) {
            return new SearchResponse("Incident","Error searching Incident", new ArrayList<>());
        }
    }

}
