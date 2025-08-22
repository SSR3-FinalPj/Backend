package org.example.ssj3pj.controller;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.services.ImageUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image,
                                              @RequestParam("locationCode") String locationCode) {
        try {
            String message = imageUploadService.uploadImageAndProcess(image, locationCode);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload image: " + e.getMessage());
        }
    }
}
