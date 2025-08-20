//// src/main/java/org/example/ssj3pj/seed/FastApiSendFromDbRunner.java
//package org.example.ssj3pj.config.runner;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.example.ssj3pj.dto.EnvironmentSummaryDto;
//import org.example.ssj3pj.entity.EnvironmentMetadata;
//import org.example.ssj3pj.repository.EnvironmentMetadataRepository;
//import org.example.ssj3pj.services.EnvironmentQueryService;
//import org.example.ssj3pj.services.VideoPromptSender;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class FastApiSendFromDbRunner implements CommandLineRunner {
//
//    private final EnvironmentMetadataRepository metadataRepository;
//    private final EnvironmentQueryService environmentQueryService; // ES -> DTO/RAW
//    private final VideoPromptSender videoPromptSender;             // FastAPI POST
//    private final ObjectMapper objectMapper = new ObjectMapper();  // DTO 로그용
//
//    @Override
//    public void run(String... args) {
//        try {
//            // 1) DB에서 가장 최신 메타데이터 1건
//            EnvironmentMetadata meta = metadataRepository.findAll()
//                    .stream()
//                    .reduce((first, second) -> second) // 마지막 요소(최신 id)
//                    .orElseThrow(() -> new IllegalStateException("메타데이터가 없습니다."));
//
//            String esDocId = meta.getEsDocId();
//            System.out.println("🗂  DB META  : id=" + meta.getId()
//                    + ", es_doc_id=" + esDocId
//                    + ", location=" + meta.getLocation()
//                    + ", recorded_at=" + meta.getRecordedAt()
//                    + ", source=" + meta.getSource());
//
//            // 2) ES 원본 JSON 로그
//            String raw = environmentQueryService.getRawSourceByDocId(esDocId);
//            System.out.println("📥 ES RAW    :\n" + raw);
//
//            // 3) DTO 매핑 후 로그
//            EnvironmentSummaryDto dto = environmentQueryService.getSummaryByDocId(esDocId);
//            String dtoJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
//            System.out.println("📦 DTO READY :\n" + dtoJson);
//
//            // 4) FastAPI로 전송
//            System.out.println("🚀 POST to FastAPI...");
//            videoPromptSender.sendEnvironmentDataToFastAPI(esDocId);
//            System.out.println("✅ FastAPI 전송 완료");
//
//        } catch (Exception e) {
//            System.err.println("❌ 전송 플로우 실패: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
