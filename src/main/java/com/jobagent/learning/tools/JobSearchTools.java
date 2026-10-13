
package com.jobagent.learning.tools;

import com.jobagent.learning.model.Job;
import com.jobagent.learning.repository.JobRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class JobSearchTools {

    private final JobRepository jobRepository;

    public JobSearchTools(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Internal Java method used to filter jobs from MySQL.
     */
    public List<Job> findMatchingJobs(
            String keyword,
            String location,
            Integer minSalaryLpa) {

        String normalizedKeyword = normalize(keyword);
        String normalizedLocation = normalize(location);

        return jobRepository.findAll().stream()
                .filter(job -> matchesKeyword(job, normalizedKeyword))
                .filter(job -> normalizedLocation == null
                        || job.getLocation().toLowerCase(Locale.ROOT)
                        .contains(normalizedLocation))
                .filter(job -> minSalaryLpa == null
                        || job.getSalaryLpa() >= minSalaryLpa)
                .toList();
    }

    /**
     * Matches a job against its title and skills.
     */
    private boolean matchesKeyword(Job job, String keyword) {
        if (keyword == null) {
            return true;
        }

        String title = job.getTitle().toLowerCase(Locale.ROOT);

        if (title.contains(keyword)) {
            return true;
        }

        boolean skillMatches = job.getSkills().stream()
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .anyMatch(skill ->
                        skill.contains(keyword) || keyword.contains(skill));

        if (skillMatches) {
            return true;
        }

        // Supports phrases such as "Java developer".
        return job.getSkills().stream()
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .anyMatch(keyword::contains);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Tool exposed to Spring AI for job searches.
     */
    @Tool(description = """
            Search available jobs by keyword, location, and minimum salary.
            The keyword can be a job title, technology, or skill.
            Examples: Java, Java Developer, Python, Spring Boot.
            Pass the minimum salary as a numeric string, such as "20".
            Use an empty string for an unspecified salary or location.
            """)
    public String searchJobs(
            String keyword,
            String location,
            String minSalaryLpa) {

        Integer minimumSalary = parseMinimumSalary(minSalaryLpa);

        List<Job> matchingJobs = findMatchingJobs(
                keyword,
                location,
                minimumSalary
        );

        if (matchingJobs.isEmpty()) {
            return "No matching jobs found.";
        }

        return matchingJobs.stream()
                .map(job -> String.format(
                        Locale.ROOT,
                        "Title: %s, Location: %s, Salary: %d LPA, "
                                + "Company: %s, Skills: %s",
                        job.getTitle(),
                        job.getLocation(),
                        job.getSalaryLpa(),
                        job.getCompany(),
                        String.join(", ", job.getSkills())
                ))
                .reduce((first, second) -> first + "\n" + second)
                .orElse("No matching jobs found.");
    }

    private Integer parseMinimumSalary(String salary) {
        if (salary == null || salary.isBlank()) {
            return null;
        }

        try {
            int value = Integer.parseInt(salary.trim());

            if (value < 0) {
                throw new IllegalArgumentException(
                        "Minimum salary cannot be negative."
                );
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Minimum salary must be a whole number, such as 20.",
                    exception
            );
        }
    }

    /**
     * Tool to retrieve details for an exact job title.
     */
    @Tool(description = "Get details for a job using its exact job title.")
    public String getJobDetails(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) {
            return "Job title is required.";
        }

        return jobRepository.findAll().stream()
                .filter(job -> job.getTitle()
                        .equalsIgnoreCase(jobTitle.trim()))
                .findFirst()
                .map(job -> String.format(
                        Locale.ROOT,
                        """
                        Job Title: %s
                        Location: %s
                        Salary: %d LPA
                        Company: %s
                        Skills: %s
                        """,
                        job.getTitle(),
                        job.getLocation(),
                        job.getSalaryLpa(),
                        job.getCompany(),
                        String.join(", ", job.getSkills())
                ))
                .orElse("No details found for job: " + jobTitle);
    }
}
