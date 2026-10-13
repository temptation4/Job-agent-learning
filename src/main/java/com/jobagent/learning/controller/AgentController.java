
package com.jobagent.learning.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.learning.model.ChatRequest;
import com.jobagent.learning.model.Job;
import com.jobagent.learning.model.JobSearchCriteria;
import com.jobagent.learning.model.NaturalLanguageRequest;
import com.jobagent.learning.tools.JobSearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final ChatClient chatClient;
    private final JobSearchTools jobSearchTools;
    private final ObjectMapper objectMapper;

    public AgentController(
            ChatClient.Builder builder,
            JobSearchTools jobSearchTools,
            ObjectMapper objectMapper) {

        this.chatClient = builder.build();
        this.jobSearchTools = jobSearchTools;
        this.objectMapper = objectMapper;
    }

    /**
     * Structured job search and job details.
     */
    @PostMapping("/jobs")
    public Object handleJobRequest(@RequestBody ChatRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required.");
        }

        String intent = request.intent() == null
                || request.intent().isBlank()
                ? "SEARCH"
                : request.intent().trim().toUpperCase(Locale.ROOT);

        if ("SEARCH".equals(intent)) {

            String result = chatClient.prompt()
                    .system("""
                            You are a job-search assistant.

                            You MUST call the searchJobs tool to search jobs.
                            Pass the supplied keyword, location and minimum salary.
                            Do not invent jobs or job details.
                            """)
                    .user("""
                            Search criteria:
                            Keyword: %s
                            Location: %s
                            Minimum salary in LPA: %s

                            An empty value means that the criterion
                            was not specified.
                            """.formatted(
                            valueOrEmpty(request.jobTitle()),
                            valueOrEmpty(request.location()),
                            request.minSalaryLpa() == null
                                    ? ""
                                    : request.minSalaryLpa()
                    ))
                    .tools(jobSearchTools)
                    .call()
                    .content();

            return result;
        }

        if ("DETAILS".equals(intent)) {

            if (request.jobTitle() == null
                    || request.jobTitle().isBlank()) {
                throw new IllegalArgumentException(
                        "jobTitle is required for DETAILS intent.");
            }

            return jobSearchTools.getJobDetails(request.jobTitle());
        }

        throw new IllegalArgumentException(
                "Unsupported intent: " + intent
                        + ". Supported intents: SEARCH, DETAILS.");
    }

    /**
     * Natural-language job search.
     */
    @PostMapping("/chat")
    public List<Job> chat(
            @RequestBody NaturalLanguageRequest request)
            throws JsonProcessingException {

        if (request == null
                || request.message() == null
                || request.message().isBlank()) {
            throw new IllegalArgumentException(
                    "message is required.");
        }

        String criteriaJson = chatClient.prompt()
                .system("""
                        Extract job-search criteria from the user's message.

                        Return exactly one JSON object with these fields:
                        {
                          "keyword": "Java",
                          "location": null,
                          "minSalaryLpa": 20
                        }

                        Rules:
                        - Extract the technology or skill as the keyword.
                        - For example, "Java developer" means keyword "Java".
                        - Set location to null unless the user explicitly
                          mentions a location in the message.
                        - Never infer a location from sample job listings
                          or previous examples.
                        - Extract minimum salary only when explicitly stated.
                        - Use null for unspecified values.
                        - Return valid JSON only, without Markdown
                          or explanations.
                        """)
                .user(request.message())
                .call()
                .content();

        String cleanedJson = cleanJsonResponse(criteriaJson);

        JobSearchCriteria criteria = objectMapper.readValue(
                cleanedJson,
                JobSearchCriteria.class);

        return jobSearchTools.findMatchingJobs(
                criteria.keyword(),
                criteria.location(),
                criteria.minSalaryLpa());
    }

    private String cleanJsonResponse(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException(
                    "Ollama returned an empty response.");
        }

        String cleaned = response.trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start == -1 || end == -1 || start > end) {
            throw new IllegalArgumentException(
                    "Ollama did not return a JSON object. Response: "
                            + cleaned);
        }

        return cleaned.substring(start, end + 1);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
