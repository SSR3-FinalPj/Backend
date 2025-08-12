package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.EnvironmentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironmentMetadataRepository extends JpaRepository<EnvironmentMetadata, Long> {

    Optional<EnvironmentMetadata> findByEsDocId(String esDocId);

    // 최근 레코드 1건 (recorded_at이 정렬 가능한 형태가 아닐 수 있으니, 우선 id Desc)
    Optional<EnvironmentMetadata> findFirstByLocationOrderByIdDesc(String location);
}
