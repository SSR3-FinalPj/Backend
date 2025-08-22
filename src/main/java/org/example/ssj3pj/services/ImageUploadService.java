package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.entity.Image;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.ImageRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.services.ES.EnvironmentQueryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender videoPromptSender;
    private final ImageRepository imageRepository;
    private final UsersRepository usersRepository;

    public String uploadImageAndProcess(MultipartFile imageFile, String locationCode, Long userId) throws IOException {
        log.info("Received image: {}", imageFile.getOriginalFilename());
        log.info("Location code: {}", locationCode);
        log.info("User ID: {}", userId);

        // 1. S3 upload logic (placeholder for now)
        // In a real scenario, this would upload to S3 and return the S3 path.
        String imagePath = "s3://your-bucket/images/" + UUID.randomUUID().toString() + "-" + imageFile.getOriginalFilename();
        log.info("Simulated S3 image path: {}", imagePath);

        // 2. Save Image entity to DB
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        Image imageEntity = Image.builder()
                .imagePath(imagePath)
                .user(user)
                .build();
        imageRepository.save(imageEntity);
        log.info("Image entity saved: {}", imagePath);

        // 3. Get environment summary from ES
        EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(locationCode);
        if (summary == null) {
            log.error("No environment summary found for location code: {}. Cannot proceed with video generation.", locationCode);
            throw new RuntimeException("No environment summary found for location code: " + locationCode);
        }

        // Set userId and imagePath in the DTO
        summary.setUserId(userId);
        summary.setImagePath(imagePath);

        // 4. Send data to FastAPI (AI service)
        videoPromptSender.sendEnvironmentDataToFastAPI(summary, userId, imagePath);
        log.info("Sent data to FastAPI for image: {} and location: {}", imagePath, locationCode);

        return "Image uploaded and processing initiated for location code: " + locationCode;
    }
}
