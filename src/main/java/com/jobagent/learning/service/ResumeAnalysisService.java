package com.jobagent.learning.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ResumeAnalysisService {

    private final ChatClient chatClient;

    public ResumeAnalysisService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String analyzeResume(
            String resume,
            String jobDescription) {

        return chatClient.prompt()
                .system("""
                    You are an expert technical recruiter.
                    Analyze resumes against job descriptions.
                    Do not invent skills or experience.
                    """)
                .user("""
                    Analyze this resume against the job description.

                    Resume:
                    %s

                    Job description:
                    %s

                    Provide:
                    1. Matching technical skills
                    2. Missing required skills
                    3. Relevant experience
                    4. Alignment with job responsibilities
                    5. Resume improvement suggestions
                    """.formatted(resume, jobDescription))
                .call()
                .content();
    }
}