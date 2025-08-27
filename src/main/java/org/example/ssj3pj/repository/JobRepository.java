package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Job findBySourceImageKey(String sourceImageKey);
}
