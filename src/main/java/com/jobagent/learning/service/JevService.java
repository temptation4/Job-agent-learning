package com.jobagent.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JevService {

    private static final String JEV_API_URL =
            "https://jev-ai.org/api/v1/systemone/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public JevService(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${jev.api-key:}") String apiKey) {

        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public JsonNode evaluateResume(
            String resume,
            String jobDescription) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Jev API key is missing. Configure JEV_API_KEY."
            );
        }

        // Build request body
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "jev-1.13");

        // Input data provided to Jev AI
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("resume", resume);
        state.put("jobDescription", jobDescription);

        requestBody.put("state", state);

        // Define questions
        Map<String, Object> questions = new LinkedHashMap<>();

        // Question 1: Resume match
        Map<String, Object> javaMatch = new LinkedHashMap<>();
        javaMatch.put("type", "noul");
        javaMatch.put(
                "instructions",
                """
                Evaluate how well the candidate's resume matches
                the Java backend job description.

                Consider Java, Spring Boot, microservices, Kafka,
                databases, and relevant professional experience.

                Return a match score between 0 and 1.
                """
        );

        questions.put("java_match", javaMatch);

        // Question 2: Job category
        Map<String, Object> jobCategory = new LinkedHashMap<>();
        jobCategory.put("type", "choice");
        jobCategory.put(
                "instructions",
                "Select the category that best describes the job."
        );

        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put(
                "java_backend",
                "Java backend development and microservices"
        );
        criteria.put(
                "full_stack",
                "Frontend and backend development"
        );
        criteria.put(
                "frontend",
                "Frontend development"
        );
        criteria.put(
                "devops",
                "Infrastructure, deployment and DevOps"
        );
        criteria.put(
                "other",
                "Other job categories"
        );

        jobCategory.put("criteria", criteria);
        questions.put("job_category", jobCategory);

        // Question 3: Detailed resume analysis
        Map<String, Object> detailedAnalysis = new LinkedHashMap<>();
        detailedAnalysis.put("type", "noul");
        detailedAnalysis.put(
                "instructions",
                """
                Analyze the candidate's resume against the job description.

                Evaluate the following:
                1. Matching technical skills.
                2. Required skills missing from the resume.
                3. Relevant professional experience.
                4. Alignment with job responsibilities.
                5. Specific suggestions to improve the resume.

                Base the analysis only on the provided resume
                and job description.
                Do not assume the candidate has skills or experience
                that are not stated.

                Return a clear, useful analysis.
                """
        );

        questions.put("detailed_analysis", detailedAnalysis);

        // Add questions to request
        requestBody.put("questions", questions);

        try {
            String response = restClient.post()
                    .uri(JEV_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new IllegalStateException(
                        "Jev API returned an empty response."
                );
            }

            return objectMapper.readTree(response);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse Jev API response.", e
            );
        }
    }
}
