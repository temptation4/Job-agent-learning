
package com.jobagent.learning.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String location;
    private int salaryLpa;
    private String company;

    @ElementCollection
    @CollectionTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id")
    )
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    protected Job() {
        // Required by JPA
    }

    public Job(String title, String location, int salaryLpa,
               String company, List<String> skills) {
        this.title = title;
        this.location = location;
        this.salaryLpa = salaryLpa;
        this.company = company;
        this.skills = new ArrayList<>(skills);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public int getSalaryLpa() {
        return salaryLpa;
    }

    public String getCompany() {
        return company;
    }

    public List<String> getSkills() {
        return List.copyOf(skills);
    }
}
