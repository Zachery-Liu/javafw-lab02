package com.lab.emp.common;

import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status).body(Result.fail(status.value(), exception.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(Result.fail(400, "参数校验失败"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception exception) {
        LOG.error("请求处理失败", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "系统繁忙，请稍后重试"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .findFirst()
                .orElse("参数校验失败");
        return handleExceptionInternal(exception, Result.fail(status.value(), message), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        if (body == null) {
            body = Result.fail(status.value(), responseMessage(status));
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    private static String responseMessage(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return "请求参数或 JSON 格式无效";
            case NOT_FOUND:
                return "接口不存在";
            case METHOD_NOT_ALLOWED:
                return "请求方法不受支持";
            case UNSUPPORTED_MEDIA_TYPE:
                return "请使用受支持的请求体格式";
            case INTERNAL_SERVER_ERROR:
                return "系统繁忙，请稍后重试";
            default:
                return status.getReasonPhrase();
        }
    }
}
