package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.JobResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobResultRepository extends JpaRepository<JobResult, Long> {

    @Query("SELECT jr FROM JobResult jr " +
            "JOIN jr.job j " +
            "WHERE j.user.id = :userId")
    List<JobResult> findAllByUserId(Long userId);
//    Optional<JobResult> findById(Long resultId);

    Optional<JobResult> findByIdAndJob_User_Id(Long resultId, Long userId);

    // userId 보안 체크용 (영상 단건 조회)
    List<JobResult> findAllByJob_User_Id(Long userId);

    // 특정 jobId 에 속한 모든 결과 조회
    List<JobResult> findAllByJobId(Long jobId);

    @Query("SELECT jr.id FROM JobResult jr " +
            "JOIN jr.job j " +
            "WHERE j.user.id = :userId " +
            "AND jr.ytUpload IS NOT NULL " +
            "AND jr.rdUpload IS NOT NULL")
    List<Long> findResultIdsUploadedToBoth(Long userId);
    JobResult findByResultKey(String resultKey);
}