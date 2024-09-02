package com.hackathon.searchIOBackend.util;

import com.hackathon.searchIOBackend.services.StackOverflow;
import org.json.JSONObject;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Service
public class OpenAI {

    private static final Logger LOGGER = Logger.getLogger(OpenAI.class.getName());
    final int MAX_TOKENS = 4096; // Total token limit for input and output combined

    private static int MAX_RESPONSE_TOKENS = 1000; // Adjust as needed for the response

    private static final String GPT_API_KEY = "gdfgfghfgfjhgfgfghf"; // Replace with your OpenAI API key
    private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";

    public String truncateTextToFitTokenLimit(String text, int length) {

        if (length <= MAX_TOKENS) {
            // Truncate the text based on the maximum token count
            String[] words = text.split("\\s+");
            StringBuilder truncatedText = new StringBuilder();
            int tokenCount = 0;

            for (String word : words) {
                tokenCount += word.length() + 1; // Approximate token count (word length + space)
                if (tokenCount > MAX_TOKENS) {
                    break;
                }
                truncatedText.append(word).append(" ");
            }

            return truncatedText.toString().trim();
        }

        else
            return text;
    }


    public int availableTokens(String text){

        int totalTokens = text.length() / 4; // Approximate token count (4 characters per token on average)
        int availableTokens = MAX_TOKENS - totalTokens - MAX_RESPONSE_TOKENS;

        // Ensure available tokens is positive
        if (availableTokens < 0) {
            availableTokens = 0;
        }
        return availableTokens;
    }

    public String gptCall(String payload) throws Exception{

            URL url = new URL(GPT_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + GPT_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));

            // Get the response code
            int responseCode = conn.getResponseCode();

        // Read the response
        BufferedReader in;
        if (responseCode >= 200 && responseCode < 300) {
            // Success: read from input stream
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            // Error: read from error stream
            in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String errorResponse = readStream(in);
            LOGGER.severe("GPT API error response: " + errorResponse);
            throw new Exception("GPT API error response: " + errorResponse);
        }

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        LOGGER.info("GPT API response: " + response.toString());

            // Parse the response
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    }

    // Utility method to read a BufferedReader stream to a String
    private String readStream(BufferedReader in) throws Exception {
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }
}
