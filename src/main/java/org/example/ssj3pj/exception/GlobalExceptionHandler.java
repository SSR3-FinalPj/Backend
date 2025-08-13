package org.example.ssj3pj.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<?> handleTokenException(TokenValidationException e) {
        return ResponseEntity
                .status(401)
                .body(new ErrorResponse("토큰 오류", e.getMessage()));
    }

    // 내부용 응답 DTO
    record ErrorResponse(String error, String message) {}
}