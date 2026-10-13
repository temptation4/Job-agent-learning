package com.jobagent.learning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ResumeAgentService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeAgentService.class);

    private final ChatClient chatClient;
    private final JevResumeScoringTool jevResumeScoringTool;

    public ResumeAgentService(
            ChatClient.Builder builder,
            JevResumeScoringTool jevResumeScoringTool) {

        this.chatClient = builder.build();
        this.jevResumeScoringTool = jevResumeScoringTool;

        log.info("ResumeAgentService initialized successfully");
    }

    public String analyzeResume(
            String resume,
            String jobDescription) {

        long startTime = System.currentTimeMillis();

        log.info("Starting resume analysis");

        log.debug("Resume length: {} characters",
                resume == null ? 0 : resume.length());

        log.debug("Job description length: {} characters",
                jobDescription == null ? 0 : jobDescription.length());

        try {
            log.info(
                    "Sending resume analysis request to Ollama with Jev scoring tool enabled");

            String result = chatClient.prompt()
                    .system("""
                            You are an expert technical recruiter.

                            You have access to a tool named scoreResumeWithJev.
                            You MUST call this tool exactly once before producing
                            your final answer.

                            Use the returned match probability and job category
                            as the authoritative Jev results.
                            Do not invent or change those values.

                            After receiving the tool result, analyze the resume
                            against the job description.

                            Include:
                            1. Jev match percentage and category
                            2. Matching technical skills
                            3. Missing or unverified required skills
                            4. Relevant experience
                            5. Alignment with job responsibilities
                            6. Specific resume improvement suggestions

                            Use only evidence from the supplied resume and
                            job description. Clearly distinguish missing skills
                            from skills that are simply not mentioned.

                            Never claim a skill is missing solely because it
                            was not explicitly mentioned in the resume.
                            """)
                    .user("""
                            Evaluate this candidate for the following role.

                            RESUME:
                            %s

                            JOB DESCRIPTION:
                            %s
                            """.formatted(resume, jobDescription))
                    .tools(jevResumeScoringTool)
                    .call()
                    .content();

            long duration = System.currentTimeMillis() - startTime;

            if (result == null || result.isBlank()) {
                log.warn(
                        "Ollama returned an empty analysis after {} ms",
                        duration);

                throw new IllegalStateException(
                        "Ollama returned an empty resume analysis");
            }

            log.info(
                    "Resume analysis completed successfully in {} ms",
                    duration);

            log.debug(
                    "Analysis response length: {} characters",
                    result.length());

            return result;

        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startTime;

            log.error(
                    "Resume analysis failed after {} ms. Exception type: {}",
                    duration,
                    ex.getClass().getSimpleName());

            throw ex;
        }
    }
}
