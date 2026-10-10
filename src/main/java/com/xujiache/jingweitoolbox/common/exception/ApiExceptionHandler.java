package com.xujiache.jingweitoolbox.common.exception;

import com.xujiache.jingweitoolbox.common.api.ErrorResponse;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.code(), exception.getMessage(), exception.details(), RequestContext.requestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return bad( "请求参数校验失败", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException exception) {
        return bad("请求参数校验失败", Map.of("violations", exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformed(Exception exception) {
        return bad("请求体或参数格式错误", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409)
                .body(new ErrorResponse("RESOURCE_CONFLICT", "资源已存在或状态冲突", null, RequestContext.requestId()));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<ErrorResponse> handleDataAccess(org.springframework.dao.DataAccessException exception) {
        return ResponseEntity.status(503)
                .body(new ErrorResponse("SERVICE_UNAVAILABLE", "数据服务暂不可用", null, RequestContext.requestId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "服务内部错误", null, RequestContext.requestId()));
    }

    private ResponseEntity<ErrorResponse> bad(String message, Map<String, Object> details) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", message, details, RequestContext.requestId()));
    }
}
