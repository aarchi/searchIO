package com.hackathon.searchIOBackend.services;

import com.hackathon.searchIOBackend.util.OpenAI;
import com.hackathon.searchIOBackend.util.OpenAIPayloads;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class Incidents {

    // Logger for logging messages
    private static final Logger logger = Logger.getLogger(Incidents.class.getName());

    @Autowired
    private OpenAI openAIService;

    @Autowired
    private OpenAIPayloads payload;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.organization.id}")
    private String organizationId;

    @Value("${openai.api.key}")
    private String apiKey;

    public List<String> searchGitLab(String searchTerm) throws Exception {
        logger.info("Starting searchGitLab method with searchTerm: " + searchTerm);

        // Prepare the final payload for the first API call
        String finalPayload = String.format(
                payload.getCitation(),
                searchTerm, openAIService.availableTokens(searchTerm)
        );
        logger.info("Prepared finalPayload for OpenAI API: " + finalPayload);

        // Make the first API call to get the GPT citation
        String gptCitation = openAIService.gptCall(finalPayload);
        logger.info("Received GPT citation: " + gptCitation);

        // Now prepare for the second API call using the GPT citation
        String requestBody = String.format("{\"history\": [{\"role\": \"user\", \"content\": \"%s\"}]}", gptCitation);
        logger.info("Prepared requestBody for second API call: " + requestBody);

        // Set up the headers for the API call
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("api-key", apiKey);
        logger.info("Headers for API call: " + headers);

        // Create the request entity with body and headers
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        logger.info("Created HttpEntity for second API call.");

        // Make the second API call and get the response
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl.replace("{organizationId}", organizationId),
                HttpMethod.POST,
                entity,
                Map.class
        );
        logger.info("Received response from second API call.");

        // Extract the final response content
        Map<String, Object> responseBody = response.getBody();
        String finalContent = (String) responseBody.get("content");
        logger.info("Extracted finalContent from response: " + finalContent);

        // Return the final content (or adjust based on what you need to return)
        return List.of(finalContent);
    }
}