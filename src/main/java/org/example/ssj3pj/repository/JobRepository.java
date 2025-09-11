package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Job findBySourceImageKey(String sourceImageKey);
    /** 사용자의 모든 Job을 생성일 역순으로 조회 */
    List<Job> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Job> findAllByParentResultId(Long resultId);
    Job findById(String id);
}
