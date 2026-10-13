
package com.jobagent.learning.model;

public record ChatRequest(
        String jobTitle,
        String location,
        String intent,
        Integer minSalaryLpa
) {
}
