
package com.jobagent.learning.model;

public record JobSearchCriteria(
        String keyword,
        String location,
        Integer minSalaryLpa
) {
}
