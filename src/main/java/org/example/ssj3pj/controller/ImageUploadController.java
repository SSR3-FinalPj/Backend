//package org.example.ssj3pj.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.ssj3pj.services.ImageUploadService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/api/upload")
//@RequiredArgsConstructor
//public class ImageUploadController {
//
//    private final ImageUploadService imageUploadService;
//
//    @PostMapping(consumes = "multipart/form-data")
//    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image,
//                                              @RequestParam("locationCode") String locationCode) {
//        try {
//            // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            Long userId = 1L; // Temporarily hardcoded for testing without JWT
////             if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
////                 userId = 1L; // Placeholder: Replace with actual logic to get the authenticated user's ID
////             }
////             if (userId == null) {
////                 return ResponseEntity.status(401).body("User not authenticated or ID not found.");
////             }
//
//            String message = imageUploadService.uploadImageAndProcess(image, locationCode, userId);
//            return ResponseEntity.ok(message);
//        } catch (IOException e) {
//            return ResponseEntity.status(500).body("Failed to upload image: " + e.getMessage());
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(400).body("Error processing request: " + e.getMessage());
//        }
//    }
//}