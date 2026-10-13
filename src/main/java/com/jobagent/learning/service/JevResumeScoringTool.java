
package com.jobagent.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JevResumeScoringTool {

    private final JevService jevService;

    public JevResumeScoringTool(JevService jevService) {
        this.jevService = jevService;
    }

    @Tool(
            name = "scoreResumeWithJev",
            description = """
            Evaluate a candidate resume against a job description using
            the Jev decision API. Returns an authoritative match probability,
            match percentage, job category and Jev response ID.
            Call this tool before making any resume matching conclusions.
            """
    )
    public Map<String, Object> scoreResume(
            @ToolParam(description = "The candidate's complete resume")
            String resume,

            @ToolParam(description = "The complete job description")
            String jobDescription
    ) {
        if (resume == null || resume.isBlank()) {
            throw new IllegalArgumentException("Resume must not be blank");
        }

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException(
                    "Job description must not be blank");
        }

        JsonNode response =
                jevService.evaluateResume(resume, jobDescription);

        JsonNode answers = response.path("answers");
        JsonNode javaMatch = answers.path("java_match");
        JsonNode jobCategory = answers.path("job_category");

        if (!javaMatch.path("noul").isNumber()) {
            throw new IllegalStateException(
                    "Jev response is missing java_match.noul");
        }

        double probability = javaMatch.path("noul").asDouble();

        if (!Double.isFinite(probability)
                || probability < 0 || probability > 1) {
            throw new IllegalStateException(
                    "Invalid Jev match probability: " + probability);
        }

        String category =
                jobCategory.path("choice").asText("unknown");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchProbability", probability);
        result.put("matchPercentage", Math.round(probability * 100));
        result.put("category", category);
        result.put("jevResponseId",
                response.path("id").asText("unknown"));

        return result;
    }
}
