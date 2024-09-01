package com.hackathon.searchIOBackend.services;

import com.hackathon.searchIOBackend.util.CleanBody;
import com.hackathon.searchIOBackend.util.OpenAI;
import com.hackathon.searchIOBackend.util.OpenAIPayloads;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class UBSAssist {
    private static final Logger LOGGER = Logger.getLogger(UBSAssist.class.getName());

    @Value("${ubs.api.url}")
    private String UBS_API_URL; // Base URL for UBS API

    @Autowired
    private CleanBody cleanBody;

    @Autowired
    private OpenAI openAI;

    @Autowired
    private OpenAIPayloads openAIPayload;

    /**
     * Searches UBS Assist API for documents related to the given search term.
     *
     * @param searchTerm The term to search for.
     * @return A map where the key is the document URL and the value is the cleaned content.
     * @throws Exception If there is an error during the API call.
     */
    public Map<String, String> searchUBSAssist(String searchTerm) throws Exception {
        Map<String, String> urlSummaryMap = new HashMap<>();
        StringBuilder aggregatedSummaries = new StringBuilder();

        try {
            // Encode the search term to be URL-safe
            String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
            LOGGER.info("Encoded search term: " + encodedSearchTerm);

            // Construct the API URL for search
            String apiUrl = String.format("%s/search?q=%s", UBS_API_URL, encodedSearchTerm);
            LOGGER.info("API URL for search: " + apiUrl);

            // Open connection to the API
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            // Check response code
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Error response from UBS API: " + responseCode);
            }

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            LOGGER.info("Search API response: " + response.toString());

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject responseObject = jsonResponse.getJSONObject("response");
            JSONArray docs = responseObject.getJSONArray("docs");

            // Process each document
            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);
                if (doc.has("url") && doc.has("content")) {
                    String url = doc.getString("url");
                    String content = doc.getString("content");

                    // Clean the content
                    String cleanedContent = cleanBody.cleanText(content);

                    // Append cleaned content to aggregatedSummaries
                    aggregatedSummaries.append(cleanedContent).append("\n\n");

                    // Add to the map
                    urlSummaryMap.put(url, cleanedContent);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error in searchUBSAssist: " + e.getMessage());
            throw new Exception("There is an issue with the UBS API call. Please try again later.", e);
        }

        if (urlSummaryMap.isEmpty()) {
            LOGGER.info("No relevant results found for the search term: " + searchTerm);
            return urlSummaryMap;
        }

        // Process the aggregated content using GPT
        String summaryOrErrorMessage = getGPTSummary(aggregatedSummaries.toString(), searchTerm, openAIPayload.getFinalSummaryPayload());
        LOGGER.info("Summary or error message: " + summaryOrErrorMessage);

        // Update the map with the final summary
        urlSummaryMap.put("summary", summaryOrErrorMessage);

        return urlSummaryMap;
    }

    /**
     * Generates a summary using the GPT model.
     *
     * @param text          The text to summarize.
     * @param context       The context for the summary.
     * @param payloadTemplate The template for the GPT payload.
     * @return The GPT-generated summary or an error message.
     */
    private String getGPTSummary(String text, String context, String payloadTemplate) {
        try {
            // Truncate text to fit token limits
            String truncatedText = openAI.truncateTextToFitTokenLimit(text, openAI.availableTokens(text));

            // Escape context and text for JSON
            String escapedContext = cleanBody.escapeJson(context);
            String escapedText = cleanBody.escapeJson(truncatedText);

            // Format the payload for GPT request
            String finalPayload = String.format(
                    payloadTemplate,
                    escapedContext, escapedText, openAI.availableTokens(escapedText)
            );
            LOGGER.info("GPT API request payload: " + finalPayload);
            return openAI.gptCall(finalPayload);
        } catch (Exception e) {
            LOGGER.severe("Error in getGPTSummary: " + e.getMessage());
            return "<h3>GPT service is not working properly.</h3> Below is the refined body received from the API calls:\n\n" + cleanBody.escapeHtml(text);
        }
    }
}
