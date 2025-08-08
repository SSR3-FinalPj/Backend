//package org.example.ssj3pj.config;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.elasticsearch.core.GetRequest;
//import co.elastic.clients.elasticsearch.core.GetResponse;
//import co.elastic.clients.elasticsearch.core.SearchRequest;
//import co.elastic.clients.elasticsearch.core.SearchResponse;
//import co.elastic.clients.elasticsearch.core.search.Hit;
//import co.elastic.clients.json.JsonData;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class ElasticsearchTestRunner implements CommandLineRunner {
//
//    private final ElasticsearchClient elasticsearchClient;
//    private final ObjectMapper objectMapper;
//
//    // ✅ ES 인덱스 내 일부 문서 ID 조회
//    public void printSomeDocIds() {
//        try {
//            SearchRequest request = new SearchRequest.Builder()
//                    .index("citydata")
//                    .query(q -> q.matchAll(m -> m)) // 전체 문서 조회
//                    .size(5) // 최대 5개만 가져오기
//                    .build();
//
//            SearchResponse<JsonData> response = elasticsearchClient.search(request, JsonData.class);
//
//            System.out.println("✅ citydata 인덱스 내 문서 ID 목록:");
//            for (Hit<JsonData> hit : response.hits().hits()) {
//                System.out.println("  → " + hit.id());
//            }
//
//        } catch (Exception e) {
//            System.out.println("❌ 문서 검색 실패: " + e.getMessage());
//        }
//    }
//
//    // ✅ 특정 문서 ID로 조회
//    public void fetchSingleDocument(String esDocId) {
//        try {
//            GetRequest getRequest = new GetRequest.Builder()
//                    .index("citydata")
//                    .id(esDocId)
//                    .build();
//
//            GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
//
//            if (response.found()) {
//                JsonNode json = objectMapper.readTree(response.source().toJson().toString());
//                System.out.println("✅ ES 문서 조회 성공!");
//                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
//            } else {
//                System.out.println("❌ ES 문서를 찾을 수 없습니다. doc_id = " + esDocId);
//            }
//
//        } catch (Exception e) {
//            System.out.println("❌ ES 조회 중 예외 발생: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void run(String... args) {
//        System.out.println("🚀 Elasticsearch 연결 테스트 시작");
//
//        // 1. 인덱스 내 문서 ID 목록 확인
//        printSomeDocIds();
//
//        // 2. 특정 문서 ID로 조회 (원하는 값으로 바꿔보기)
//        String esDocId = "Y5QSg5gB16D9HeeTnVoC";
//        fetchSingleDocument(esDocId);
//    }
//}
