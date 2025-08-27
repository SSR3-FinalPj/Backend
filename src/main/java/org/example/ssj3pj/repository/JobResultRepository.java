package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.JobResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobResultRepository extends JpaRepository<JobResult, Long> {
}