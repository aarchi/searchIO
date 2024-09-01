package com.hackathon.searchIOBackend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenAIPayloads {

    public String getFinalSummaryPayload() {
        String payload =
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"You are a highly intelligent assistant. Format your response using HTML. Include a heading with the user's question in an <h5> tag and provide the answer in a <div> tag. Use the <pre> tag for code snippets. Remove any unwanted special characters or symbols from the response. Ensure that the response is clear, logically consistent, and visually appealing. The content should be based solely on the provided text, but use common sense to enhance clarity.\"}, " +
                        "{\"role\": \"user\", \"content\": \"Question: %s\\n\\nSummary: %s\"}" +
                        "], \"max_tokens\": %d}";

        return payload;
    }

    public String getCitation() {
        String payload =
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"You are an advanced AI assistant. The API you are working with deals with documents that record production issues faced by various teams at UBS. These documents include information about incidents, their impacts, and descriptions. Based on the user's query, generate a concise and specific search query that targets these types of documents. Ensure the query is clear, avoids general document summaries, multi-hop reasoning, and listing/enumeration, and focuses on retrieving specific information related to incidents, impacts, or descriptions. Remove any unwanted special characters.\"}, " +
                        "{\"role\": \"user\", \"content\": \"Original Query: %s\\n\\nContext: The documents contain incident numbers, impacts, and descriptions of production issues faced by UBS teams. Rules: Avoid summaries, multi-hop reasoning, and lists. Generate a precise search query around the user's searched string.\"}" +
                        "], \"max_tokens\": %d}";
        return payload;
    }




}
