package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.EnvironmentMetadata;
import org.example.ssj3pj.repository.EnvironmentMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class EnvironmentDataService {

    private final EnvironmentMetadataRepository metadataRepository;
    private final EnvironmentQueryService environmentQueryService;

    /**
     * 1) 메타데이터 PK(id)로 조회 → 해당 es_doc_id로 ES 조회
     */
    @Transactional(readOnly = true)
    public EnvironmentSummaryDto getByMetaId(Long metaId) {
        EnvironmentMetadata meta = metadataRepository.findById(metaId)
                .orElseThrow(() -> new IllegalArgumentException("메타데이터 없음: id=" + metaId));

        return environmentQueryService.getSummaryByDocId(meta.getEsDocId());
    }

    /**
     * 2) location의 최신 메타데이터 1건 기준 → ES 조회
     *    (recorded_at 정렬이 어려우면 id desc 기준으로 최신 취급)
     */
    @Transactional(readOnly = true)
    public EnvironmentSummaryDto getLatestByLocation(String location) {
        EnvironmentMetadata meta = metadataRepository.findFirstByLocationOrderByIdDesc(location)
                .orElseThrow(() -> new IllegalArgumentException("해당 위치의 메타데이터 없음: location=" + location));

        return environmentQueryService.getSummaryByDocId(meta.getEsDocId());
    }

    /**
     * 3) DB에 존재하는 es_doc_id인지 검증 후 ES 조회 (선택)
     */
    @Transactional(readOnly = true)
    public EnvironmentSummaryDto getByEsDocIdFromDb(String esDocId) {
        metadataRepository.findByEsDocId(esDocId)
                .orElseThrow(() -> new IllegalArgumentException("DB에 없는 es_doc_id: " + esDocId));

        return environmentQueryService.getSummaryByDocId(esDocId);
    }

    /**
     * 4) EnvironmentMetadata 저장
     */
    @Transactional
    public void save(EnvironmentMetadata metadata) {
        metadataRepository.save(metadata);
    }

    /**
     * 5) EnvironmentMetadata 일괄 저장
     */
    @Transactional
    public void saveAll(List<EnvironmentMetadata> metadataList) {
        metadataRepository.saveAll(metadataList);
    }
}
