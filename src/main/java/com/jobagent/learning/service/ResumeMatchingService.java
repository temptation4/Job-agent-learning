package com.jobagent.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResumeMatchingService {

    private final JevService jevService;
    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeMatchingService(
            JevService jevService,
            ResumeAnalysisService resumeAnalysisService) {
        this.jevService = jevService;
        this.resumeAnalysisService = resumeAnalysisService;
    }

    public Map<String, Object> matchResume(
            String resume,
            String jobDescription) {

        // 1. Get match score and job category from Jev
        JsonNode response =
                jevService.evaluateResume(resume, jobDescription);

        JsonNode answers = response.path("answers");
        JsonNode javaMatch = answers.path("java_match");
        JsonNode jobCategory = answers.path("job_category");

        if (!javaMatch.path("noul").isNumber()) {
            throw new IllegalStateException(
                    "Jev response is missing java_match.noul: "
                            + response);
        }

        double probability = javaMatch.path("noul").asDouble();

        if (probability < 0 || probability > 1) {
            throw new IllegalStateException(
                    "Invalid Java match probability: " + probability);
        }

        String category =
                jobCategory.path("choice").asText("unknown");

        // 2. Generate detailed analysis using Ollama
        String detailedAnalysis =
                resumeAnalysisService.analyzeResume(
                        resume,
                        jobDescription);

        // 3. Build the final response
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("matchProbability", probability);
        result.put(
                "matchPercentage",
                Math.round(probability * 100)
        );
        result.put("category", category);

        result.put(
                "recommendation",
                probability >= 0.80
                        ? "Proceed to detailed analysis"
                        : "Review the resume against job requirements"
        );

        result.put(
                "jevResponseId",
                response.path("id").asText("unknown")
        );

        result.put("detailedAnalysis", detailedAnalysis);

        return result;
    }
}
