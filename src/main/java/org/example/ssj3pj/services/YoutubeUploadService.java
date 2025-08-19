// src/main/java/org/example/ssj3pj/services/YoutubeUploadService.java
package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeUploadService {

    private final GoogleTokenService googleTokenService;
    private final  RestTemplate youtubeRestTemplate;

    @Value("${youtube.upload.chunk-size-bytes:8388608}")
    private long chunkSize; // 8MB

    /**
     * 업로드 완료 시 YouTube video resource JSON 문자열을 반환합니다.
     * - userId: AT를 가져올 사용자
     * - filePath: 서버 로컬의 영상 파일 경로
     */
    public String upload(Long userId,
                         String filePath,
                         String title,
                         String description,
                         List<String> tags,
                         String privacyStatus, // "private"|"unlisted"|"public"
                         String categoryId,    // 예: "22"
                         Boolean madeForKids) throws Exception {

        Path file = Path.of(filePath);
        long fileSize = Files.size(file);
        String mime = Files.probeContentType(file);
        if (mime == null) mime = "video/*";

        // 1) 유효 AT 확보 (임박 시 자동 refresh)
        String accessToken = googleTokenService.getValidAccessToken(userId);

        // 2) Resumable 세션 시작
        String uploadUrl = initiateResumable(accessToken, fileSize, mime, title, description, tags, privacyStatus, categoryId, madeForKids);

        // 3) 청크 업로드 (401/네트워크 오류/308 대응)
        return putChunksWithResume(uploadUrl, accessToken, userId, file, fileSize);
    }

    /** Resumable 업로드 세션 시작 → Location(uploadUrl) 반환 */
    private String initiateResumable(String accessToken,
                                     long fileSize,
                                     String mime,
                                     String title,
                                     String description,
                                     List<String> tags,
                                     String privacyStatus,
                                     String categoryId,
                                     Boolean madeForKids) {

        String initUrl = "https://www.googleapis.com/upload/youtube/v3/videos"
                + "?uploadType=resumable&part=snippet,status";

        Map<String, Object> snippet = new HashMap<>();
        snippet.put("title", title);
        snippet.put("description", description == null ? "" : description);
        snippet.put("tags", tags == null ? List.of() : tags);
        if (categoryId != null && !categoryId.isBlank()) {
            snippet.put("categoryId", categoryId);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("privacyStatus", privacyStatus == null ? "private" : privacyStatus);
        if (madeForKids != null) {
            status.put("madeForKids", madeForKids);
            status.put("selfDeclaredMadeForKids", madeForKids);
        }

        Map<String, Object> meta = Map.of("snippet", snippet, "status", status);

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("X-Upload-Content-Type", mime);
        h.add("X-Upload-Content-Length", String.valueOf(fileSize));

        ResponseEntity<Void> res = youtubeRestTemplate.exchange(
                initUrl, HttpMethod.POST, new HttpEntity<>(meta, h), Void.class);

        String uploadUrl = res.getHeaders().getFirst("Location");
        if (uploadUrl == null) throw new IllegalStateException("Missing resumable upload URL (Location header)");
        log.info("[YT] init OK, uploadUrl={}", uploadUrl);
        return uploadUrl;
    }

    /** 청크 업로드 루프 (308/401/네트워크 오류 처리 + 오프셋 재탐색) */
    private String putChunksWithResume(String uploadUrl,
                                       String accessTokenInitial,
                                       Long userId,
                                       Path file,
                                       long fileSize) throws Exception {
        String accessToken = accessTokenInitial;

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            long offset = 0;

            while (offset < fileSize) {
                int toRead = (int) Math.min(chunkSize, fileSize - offset);
                byte[] data = readChunk(ch, offset, toRead);

                int attempt = 0;
                while (true) {
                    try {
                        String contentRange = "bytes " + offset + "-" + (offset + toRead - 1) + "/" + fileSize;

                        HttpHeaders h = new HttpHeaders();
                        h.setBearerAuth(accessToken);
                        h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                        h.setContentLength(toRead);
                        h.add("Content-Range", contentRange);

                        ResponseEntity<String> put = youtubeRestTemplate.exchange(
                                uploadUrl, HttpMethod.PUT,
                                new HttpEntity<>(data, h), String.class);

                        int code = put.getStatusCodeValue();

                        if (code == 308) { // Resume Incomplete
                            String range = put.getHeaders().getFirst("Range"); // e.g. "bytes=0-8388607"
                            long next = computeNextOffset(range, offset, toRead);
                            log.debug("[YT] 308 resume, range={}, nextOffset={}", range, next);
                            offset = next;
                            break; // 다음 청크로
                        }

                        if (put.getStatusCode().is2xxSuccessful()) {
                            log.info("[YT] upload complete, size={} bytes", fileSize);
                            return put.getBody(); // video resource JSON
                        }

                        throw new RuntimeException("Unexpected response: " + put.getStatusCode());

                    } catch (HttpClientErrorException.Unauthorized e) {
                        // 401: AT 만료/무효 → 최신 AT로 교체 후 같은 청크 재시도
                        if (++attempt > 3) throw e;
                        accessToken = googleTokenService.getValidAccessToken(userId);
                        log.warn("[YT] 401 → refreshed AT, retry chunk. attempt={}", attempt);

                    } catch (ResourceAccessException | HttpServerErrorException e) {
                        // 네트워크/서버 오류 → 오프셋 질의 후 재개
                        if (++attempt > 3) throw e;
                        long serverOffset = probeNextOffset(uploadUrl, accessToken, fileSize);
                        if (serverOffset >= 0 && serverOffset != offset) {
                            log.warn("[YT] network issue → serverOffset={}, localOffset={}", serverOffset, offset);
                            offset = serverOffset;
                        }
                        Thread.sleep(Duration.ofSeconds(2L * attempt).toMillis());
                    }
                }

                // 진행 로그(선택)
                double pct = (offset * 100.0) / fileSize;
                log.info("[YT] progress: {}% ({} / {} bytes)", String.format("%.2f", pct), offset, fileSize);
            }
        }

        throw new IllegalStateException("Upload did not complete (no 2xx received)");
    }

    /** 서버가 회신한 Range 헤더로 다음 offset 계산 */
    private long computeNextOffset(String rangeHeader, long currentOffset, int sentBytes) {
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            try {
                String[] parts = rangeHeader.substring("bytes=".length()).split("-");
                long last = Long.parseLong(parts[1]); // 마지막 수신 바이트 인덱스
                return last + 1;
            } catch (Exception ignore) { /* fallback below */ }
        }
        return currentOffset + sentBytes; // Range가 없으면 보낸 만큼 증가
    }

    /** PUT zero-length로 오프셋 질의: 308 + Range 수신 기대 */
    private long probeNextOffset(String uploadUrl, String accessToken, long fileSize) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(accessToken);
            h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            h.setContentLength(0);
            h.add("Content-Range", "bytes */" + fileSize);

            ResponseEntity<String> res = youtubeRestTemplate.exchange(
                    uploadUrl, HttpMethod.PUT, new HttpEntity<>(new byte[0], h), String.class);

            return -1; // 정상적이면 여기 오지 않음(보통 308)
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 308) {
                String range = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Range") : null;
                if (range != null && range.startsWith("bytes=")) {
                    String[] parts = range.split("=");
                    String[] nums = parts[1].split("-");
                    long last = Long.parseLong(nums[1]);
                    return last + 1;
                }
                return 0; // 아무 것도 안 받은 상태
            }
            return -1; // 그 외: 세션 만료 등 → 상위에서 새 세션 생성 고려
        }
    }

    /** 파일에서 청크 읽기 */
    private byte[] readChunk(FileChannel ch, long offset, int size) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(size);
        int read = ch.read(buf, offset);
        if (read < 0) return new byte[0];
        if (read != size) {
            byte[] partial = new byte[read];
            buf.flip();
            buf.get(partial);
            return partial;
        }
        return buf.array();
    }
}
