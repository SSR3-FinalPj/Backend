package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
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
public class ImageUploadService {

    private final EnvironmentQueryService environmentQueryService;
    private final VideoPromptSender videoPromptSender;
    private final ImageRepository imageRepository;
    private final UsersRepository usersRepository;

    public String uploadImageAndProcess(String imageKey, String locationCode, String userName){
        try{


            // 2. Save Image entity to DB
            Users user = usersRepository.findByUsername(userName)
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + userName));

            Image imageEntity = Image.builder()
                    .imageKey(imageKey)
                    .user(user)
                    .locationCode(locationCode)
                    .build();
            imageRepository.save(imageEntity);

            // 3. Get environment summary from ES
            EnvironmentSummaryDto summary = environmentQueryService.getRecentSummaryByLocation(locationCode);
            if (summary == null) {
                throw new RuntimeException("No environment summary found for location code: " + locationCode);
            }

            // Set userId and imageKey in the DTO
            summary.setUserId(user.getId());
            summary.setImageKey(imageKey);

            // 4. Send data to FastAPI (AI service)
            videoPromptSender.sendEnvironmentDataToFastAPI(summary, user.getId(), imageKey);

            return "Image uploaded and processing initiated for location code: " + locationCode;

        }catch (Exception e) {
            throw new RuntimeException("Error during image processing", e);
        }
    }
}
