package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final EnvironmentQueryService environmentQueryService;

    public String uploadImageAndProcess(MultipartFile image, String locationCode) throws IOException {
        // For now, just log the information.
        // S3 upload logic will be added later.
        log.info("Received image: {}", image.getOriginalFilename());
        log.info("Location code: {}", locationCode);

        // The method now returns a single averaged DTO, or null.
        EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(locationCode);

        log.info("Summary for location code {}: {}", locationCode, summary);

        int count = (summary == null) ? 0 : 1;
        return "Image and location code received successfully. Found " + count + " summary.";
    }
}