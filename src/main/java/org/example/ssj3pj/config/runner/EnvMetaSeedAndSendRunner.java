//package org.example.ssj3pj.config.runner;
//
//import lombok.RequiredArgsConstructor;
//import org.example.ssj3pj.entity.EnvironmentMetadata;
//import org.example.ssj3pj.repository.EnvironmentMetadataRepository;
//import org.example.ssj3pj.services.VideoPromptSender;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//@RequiredArgsConstructor
//public class EnvMetaSeedAndSendRunner implements CommandLineRunner {
//
//    private final EnvironmentMetadataRepository repository;
//    private final VideoPromptSender videoPromptSender;
//
//    @Override
//    public void run(String... args) {
//        try {
//            // ✅ 1) Seed 데이터 생성 (필요 시)
//            String esDocId = "05TPlpgB16D9HeeTwlti";
//            boolean exists = repository.findByEsDocId(esDocId).isPresent();
//            if (!exists) {
//                EnvironmentMetadata row = EnvironmentMetadata.builder()
//                        .esDocId(esDocId)
//                        .location("익선동")                // 필요 시 수정
//                        .recordedAt("2025-08-06 15:30")  // 필요 시 수정
//                        .indexedAt(LocalDateTime.now())
//                        .source("citydata")
//                        .build();
//
//                repository.save(row);
//                System.out.println("✅ Seed 데이터 저장 완료: es_doc_id = " + esDocId);
//            } else {
//                System.out.println("⚠️ Seed 데이터 이미 존재: es_doc_id = " + esDocId);
//            }
//
//            // ✅ 2) DB에서 최신 메타데이터 조회
//            EnvironmentMetadata latestMeta = repository.findAll()
//                    .stream()
//                    .reduce((first, second) -> second) // 마지막 요소(최신 id)
//                    .orElseThrow(() -> new IllegalStateException("메타데이터가 없습니다."));
//
//            String latestEsDocId = latestMeta.getEsDocId();
//            System.out.println("🔎 최신 es_doc_id = " + latestEsDocId);
//
//            // ✅ 3) FastAPI로 전송
//            videoPromptSender.sendEnvironmentDataToFastAPI(latestEsDocId);
//
//        } catch (Exception e) {
//            System.err.println("❌ 실행 실패: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
