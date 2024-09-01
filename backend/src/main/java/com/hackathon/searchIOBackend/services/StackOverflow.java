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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class StackOverflow {

    private static final Logger LOGGER = Logger.getLogger(StackOverflow.class.getName());

    @Value("${stack.api.key}")
    private String API_KEY; // Replace with your API key

    @Value("${stack.api.url}")
    private String STACK_API_URL;

    @Value("${stack.api.q.url}")
    private String STACK_API_Q_URL;

    @Value("${stack.max.urls}")
    private int MAX_URLS;



    @Autowired
    private CleanBody cleanBody;

    @Autowired
    private OpenAI openAI;

    @Autowired
    private OpenAIPayloads openAIPayload;

    /**
     * Searches Stack Overflow for questions based on the search term.
     *
     * @param searchTerm The term to search for in Stack Overflow.
     * @return A list of URLs for the questions found.
     * @throws Exception If an error occurs during the search.
     */
    public List<String> searchStackOverflow(String searchTerm) throws Exception {
        List<String> questionIds = new ArrayList<>();
        try {
            String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
            LOGGER.info("Encoded search term: " + encodedSearchTerm);

            String apiUrl = String.format(
                    "%s/search/advanced?order=desc&sort=relevance&q=%s&accepted=True&site=stackoverflow&key=%s",
                    STACK_API_URL, encodedSearchTerm, API_KEY);
            LOGGER.info("API URL for search: " + apiUrl);

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            LOGGER.info("Response code from Stack Overflow search API: " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Error response from Stack Overflow API: " + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            LOGGER.info("Search API response: " + response.toString());

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject question = items.getJSONObject(i);
                questionIds.add(String.valueOf(question.getInt("question_id")));
                if (questionIds.size() >= MAX_URLS) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error in searchStackOverflow: " + e.getMessage());
            throw new Exception("There is an issue with the Stack Overflow API call. Please try again later.", e);
        }

        List<String> urls = new ArrayList<>();
        for (String questionId : questionIds) {
            String questionUrl = STACK_API_Q_URL + questionId;
            urls.add(questionUrl);
            LOGGER.info("Constructed question URL: " + questionUrl);
        }
        return urls;
    }

    /**
     * Fetches and summarizes the bodies of answers from a list of Stack Overflow question URLs.
     *
     * @param questionUrls The list of question URLs.
     * @param context Additional context for summarization.
     * @return A summarized text of the answers.
     */
    public String fetchAndSummarizeBodies(List<String> questionUrls, String context) {
        StringBuilder aggregatedSummaries = new StringBuilder();

        try {
            for (String questionUrl : questionUrls) {
                String[] urlParts = questionUrl.split("/");
                if (urlParts.length < 5) {
                    LOGGER.warning("URL format is incorrect: " + questionUrl);
                    continue;
                }
                String questionId = urlParts[4];

                String apiUrl = String.format(
                        "%s/questions/%s/answers?order=desc&sort=activity&site=stackoverflow&filter=withbody",
                        STACK_API_URL, questionId);
                LOGGER.info("API URL for fetching answers: " + apiUrl);

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                LOGGER.info("Response code from Stack Overflow answers API: " + responseCode);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();

                    LOGGER.severe("Error response from Stack Overflow API: " + responseCode);
                    LOGGER.severe("Error details: " + errorResponse.toString());

                    throw new IOException("Error response from Stack Overflow API: " + responseCode);
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                LOGGER.info("Fetch answers API response: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray items = jsonResponse.getJSONArray("items");

                for (int j = 0; j < items.length(); j++) {
                    JSONObject answer = items.getJSONObject(j);
                    if (answer.has("body")) {
                        String body = answer.getString("body");
                        LOGGER.info("Original answer body: " + body);

                        body = cleanBody.cleanText(body);
                        LOGGER.info("Cleaned answer body: " + body);

                        aggregatedSummaries.append(body).append("\n\n");
                    }
                }
            }

            return getGPTSummary(aggregatedSummaries.toString(), context, openAIPayload.getFinalSummaryPayload());
        } catch (Exception e) {
            LOGGER.severe("Error in fetchAndSummarizeBodies: " + e.getMessage());
            return "The following URLs were retrieved but could not be analyzed due to a technical error.";
        }
    }

    /**
     * Gets a summary of the given text using GPT.
     *
     * @param text The text to summarize.
     * @param context Additional context for summarization.
     * @param payloadTemplate The template for the GPT API payload.
     * @return The summary of the text.
     */
    private String getGPTSummary(String text, String context, String payloadTemplate) {
        try {
            String truncatedText = openAI.truncateTextToFitTokenLimit(text, openAI.availableTokens(text));

            String escapedContext = cleanBody.escapeJson(context);
            String escapedText = cleanBody.escapeJson(truncatedText);

            String finalPayload = String.format(
                    payloadTemplate,
                    escapedContext, escapedText, openAI.availableTokens(escapedText)
            );
            LOGGER.info("GPT API request payload: " + finalPayload);
            String gptResponse = openAI.gptCall(finalPayload);
            LOGGER.info("GPT API response: " + gptResponse);
            return gptResponse;
        } catch (Exception e) {
            LOGGER.severe("Error in getGPTSummary: " + e.getMessage());
            return "GPT service is not working properly. Below is the refined body received from the API calls:\n" + cleanBody.escapeHtml(text);
        }
    }
}
