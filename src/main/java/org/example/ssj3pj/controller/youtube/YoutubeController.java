//import org.example.ssj3pj.dto.youtube.YoutubeSummaryDto;
//import org.example.ssj3pj.entity.YoutubeMetadata;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//package org.example.ssj3pj.controller.youtube;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.ssj3pj.dto.youtube.YoutubeWithDetailsDto;
//import org.example.ssj3pj.dto.youtube.YoutubeSummaryDto;
//import org.example.ssj3pj.entity.YoutubeMetadata;
//import org.example.ssj3pj.repository.YoutubeMetadataRepository;
//import org.example.ssj3pj.services.ES.YoutubeQueryService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/youtube")
//@RequiredArgsConstructor
//@Slf4j
//public class YoutubeController {
//
//    private final YoutubeMetadataRepository youtubeMetadataRepository;
//    private final YoutubeQueryService youtubeQueryService;
//
//    /**
//     * 모든 YouTube 메타데이터와 ES 데이터를 결합하여 조회
//     */
//    @GetMapping("/list-with-details")
//    public ResponseEntity<List<YoutubeWithDetailsDto>> getYoutubeDataWithDetails() {
//        try {
//            // 1. DB에서 모든 YouTube 메타데이터 조회
//            List<YoutubeMetadata> metadataList = youtubeMetadataRepository.findAll();
//
//            // 2. 각 es_doc_id로 ES에서 실제 데이터 조회하여 결합
//            List<YoutubeWithDetailsDto> results = metadataList.stream()
//                    .map(metadata -> {
//                        try {
//                            YoutubeSummaryDto esData = youtubeQueryService.getSummaryByDocId(metadata.getEsDocId());
//                            return YoutubeWithDetailsDto.fromMetadataAndEs(metadata, esData);
//                        } catch (Exception e) {
//                            log.error("❌ ES 데이터 조회 실패 for esDocId: {}", metadata.getEsDocId(), e);
//                            // ES 데이터가 없어도 메타데이터는 반환
//                            return YoutubeWithDetailsDto.fromMetadataAndEs(metadata, null);
//                        }
//                    })
//                    .collect(Collectors.toList());
//
//            return ResponseEntity.ok(results);
//        } catch (Exception e) {
//            log.error("❌ YouTube 데이터 조회 중 오류", e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    /**
//     * 특정 YouTube ID로 메타데이터와 ES 데이터 조회
//     */
//    @GetMapping("/youtube-id/{youtubeId}")
//    public ResponseEntity<YoutubeWithDetailsDto> getYoutubeDataByYoutubeId(@PathVariable String youtubeId) {
//        try {
//            // 1. DB에서 메타데이터 조회
//            YoutubeMetadata metadata = youtubeMetadataRepository.findByYoutubeId(youtubeId)
//                    .orElseThrow(() -> new RuntimeException("YouTube 메타데이터 없음: " + youtubeId));
//
//            // 2. ES에서 실제 데이터 조회
//            YoutubeSummaryDto esData = youtubeQueryService.getSummaryByDocId(metadata.getEsDocId());
//
//            YoutubeWithDetailsDto result = YoutubeWithDetailsDto.fromMetadataAndEs(metadata, esData);
//
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            log.error("❌ YouTube 데이터 조회 실패 for youtubeId: {}", youtubeId, e);
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * ES Document ID로 직접 조회
//     */
//    @GetMapping("/es-doc/{esDocId}")
//    public ResponseEntity<YoutubeSummaryDto> getYoutubeDataByEsDocId(@PathVariable String esDocId) {
//        try {
//            YoutubeSummaryDto esData = youtubeQueryService.getSummaryByDocId(esDocId);
//            return ResponseEntity.ok(esData);
//        } catch (Exception e) {
//            log.error("❌ ES 데이터 조회 실패 for esDocId: {}", esDocId, e);
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * ES 원본 데이터 조회 (JSON 객체로 반환)
//     */
//    @GetMapping("/raw/{esDocId}")
//    public Object getRawYoutubeData(@PathVariable String esDocId) {
//        try {
//            // ES에서 JSON 객체로 직접 반환 (실행 중인 동작과 일치)
//            return youtubeQueryService.getRawYoutubeByEsDocId(esDocId);
//        } catch (Exception e) {
//            log.error("❌ ES 원본 데이터 조회 실패 for esDocId: {}", esDocId, e);
//            throw new RuntimeException("ES 데이터 조회 실패: " + esDocId);
//        }
//    }
//
//    /**
//     * DB 메타데이터 전체 조회
//     */
//    @GetMapping("/metadata")
//    public List<YoutubeMetadata> getAllMetadata() {
//        try {
//            return youtubeMetadataRepository.findAll();
//        } catch (Exception e) {
//            log.error("❌ DB 메타데이터 조회 실패", e);
//            throw new RuntimeException("DB 조회 실패: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 특정 ID로 메타데이터 조회
//     */
//    @GetMapping("/metadata/{id}")
//    public ResponseEntity<YoutubeMetadata> getMetadataById(@PathVariable Long id) {
//        try {
//            YoutubeMetadata metadata = youtubeMetadataRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("메타데이터 없음: " + id));
//            return ResponseEntity.ok(metadata);
//        } catch (Exception e) {
//            log.error("❌ 메타데이터 조회 실패 for id: {}", id, e);
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * 특정 ID로 메타데이터와 ES 데이터 통합 조회
//     */
//    @GetMapping("/with-details/{id}")
//    public ResponseEntity<YoutubeWithDetailsDto> getWithDetailsById(@PathVariable Long id) {
//        try {
//            // 1. DB에서 메타데이터 조회
//            YoutubeMetadata metadata = youtubeMetadataRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("메타데이터 없음: " + id));
//
//            // 2. ES에서 실제 데이터 조회
//            YoutubeSummaryDto esData = youtubeQueryService.getSummaryByDocId(metadata.getEsDocId());
//
//            YoutubeWithDetailsDto result = YoutubeWithDetailsDto.fromMetadataAndEs(metadata, esData);
//
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            log.error("❌ 통합 데이터 조회 실패 for id: {}", id, e);
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//}
//*/
