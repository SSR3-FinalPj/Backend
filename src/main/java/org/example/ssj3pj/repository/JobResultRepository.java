package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.JobResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobResultRepository extends JpaRepository<JobResult, Long> {

    @Query("SELECT jr FROM JobResult jr " +
            "JOIN jr.job j " +
            "WHERE j.user.id = :userId")
    List<JobResult> findAllByUserId(Long userId);
}