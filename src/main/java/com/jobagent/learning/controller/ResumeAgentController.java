
package com.jobagent.learning.controller;

import com.jobagent.learning.model.ResumeAgentRequest;
import com.jobagent.learning.service.ResumeAgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/resume")
public class ResumeAgentController {

    private final ResumeAgentService resumeAgentService;

    public ResumeAgentController(
            ResumeAgentService resumeAgentService) {
        this.resumeAgentService = resumeAgentService;
    }

    @PostMapping("/agent-analysis")
    public String analyzeResume(
            @RequestBody ResumeAgentRequest request) {

        if (request.resume() == null
                || request.resume().isBlank()
                || request.jobDescription() == null
                || request.jobDescription().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume and jobDescription are required"
            );
        }

        return resumeAgentService.analyzeResume(
                request.resume(),
                request.jobDescription()
        );
    }
}
