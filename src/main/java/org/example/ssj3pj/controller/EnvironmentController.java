package org.example.ssj3pj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.EnvironmentSummaryDto;
import org.example.ssj3pj.services.EnvironmentESService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/environment")
public class EnvironmentController {

    private final EnvironmentESService environmentESService;
    @Tag(name = "ES-Seoulrealtime", description = "서울시 실시간데이터")
    @GetMapping("/{esDocId}")
    public ResponseEntity<EnvironmentSummaryDto> getByDocId(@PathVariable String esDocId) {
        EnvironmentSummaryDto dto = environmentESService.getEnvironmentDataByDocId(esDocId);
        return ResponseEntity.ok(dto);
    }
}