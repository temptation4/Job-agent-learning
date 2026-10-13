
package com.jobagent.learning.controller;

import com.jobagent.learning.service.ResumeMatchingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeMatchingService matchingService;

    public ResumeController(
            ResumeMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/match")
    public Map<String, Object> match(
            @RequestBody ResumeMatchRequest request) {

        return matchingService.matchResume(
                request.resume(),
                request.jobDescription()
        );
    }

    public record ResumeMatchRequest(
            String resume,
            String jobDescription
    ) {}
}
