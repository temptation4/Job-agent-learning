
package com.jobagent.learning.repository;

import com.jobagent.learning.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}
