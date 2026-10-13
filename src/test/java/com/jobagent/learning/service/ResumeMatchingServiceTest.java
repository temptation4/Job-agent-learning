package com.jobagent.learning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeMatchingServiceTest {

    @Mock
    private JevService jevService;

    @Mock
    private ResumeAnalysisService resumeAnalysisService;

    @InjectMocks
    private ResumeMatchingService resumeMatchingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnMatchScoreCategoryAndDetailedAnalysis()
            throws Exception {

        String json = """
            {
              "id": "jev-test-123",
              "answers": {
                "java_match": {
                  "noul": 0.90
                },
                "job_category": {
                  "choice": "java_backend"
                }
              }
            }
            """;

        JsonNode jevResponse = objectMapper.readTree(json);

        when(jevService.evaluateResume("Java developer", "Spring Boot role"))
                .thenReturn(jevResponse);

        when(resumeAnalysisService.analyzeResume(
                "Java developer", "Spring Boot role"))
                .thenReturn("Strong Java match; Docker experience is missing.");

        Map<String, Object> result =
                resumeMatchingService.matchResume(
                        "Java developer", "Spring Boot role");

        assertEquals(0.90, result.get("matchProbability"));
        assertEquals(90L, result.get("matchPercentage"));
        assertEquals("java_backend", result.get("category"));
        assertEquals("jev-test-123", result.get("jevResponseId"));
        assertEquals(
                "Strong Java match; Docker experience is missing.",
                result.get("detailedAnalysis"));

        verify(jevService).evaluateResume(
                "Java developer", "Spring Boot role");
        verify(resumeAnalysisService).analyzeResume(
                "Java developer", "Spring Boot role");
    }

    @Test
    void shouldRejectInvalidMatchProbability() throws Exception {

        String json = """
            {
              "id": "jev-test-456",
              "answers": {
                "java_match": {
                  "noul": 1.5
                },
                "job_category": {
                  "choice": "java_backend"
                }
              }
            }
            """;

        when(jevService.evaluateResume("Resume", "Job"))
                .thenReturn(objectMapper.readTree(json));

        assertThrows(
                IllegalStateException.class,
                () -> resumeMatchingService.matchResume("Resume", "Job"));

        verifyNoInteractions(resumeAnalysisService);
    }

    @Test
    void shouldRejectMissingMatchProbability() throws Exception {

        String json = """
            {
              "id": "jev-test-789",
              "answers": {
                "job_category": {
                  "choice": "java_backend"
                }
              }
            }
            """;

        when(jevService.evaluateResume("Resume", "Job"))
                .thenReturn(objectMapper.readTree(json));

        assertThrows(
                IllegalStateException.class,
                () -> resumeMatchingService.matchResume("Resume", "Job"));

        verifyNoInteractions(resumeAnalysisService);
    }
}
