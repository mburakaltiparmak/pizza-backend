package com.example.pizza.exceptions;

import com.example.pizza.dto.exceptions.ApiError;
import com.example.pizza.dto.exceptions.ExceptionResponse;
import com.example.pizza.dto.product.StockErrorResponse;
import com.example.pizza.exceptions.base.ApiException;
import com.example.pizza.exceptions.base.ValidationException;
import com.example.pizza.exceptions.common.AccessDeniedException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.exceptions.order.InsufficientStockException;
import com.example.pizza.exceptions.order.OrderCreationException;
import com.example.pizza.exceptions.order.PaymentProcessingException;
import com.example.pizza.exceptions.user.UserAuthenticationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class ApiGlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionResponse> handleApiException(ApiException exception) {
        log.error("API Exception occurred: {}", exception.getMessage(), exception);
        ExceptionResponse response = new ExceptionResponse(
                exception.getMessage(),
                exception.getHttpStatus().value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, exception.getHttpStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
        log.error("Resource not found: {}", exception.getMessage(), exception);
        ExceptionResponse response = new ExceptionResponse(
                exception.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAuthenticationException.class)
    public ResponseEntity<ExceptionResponse> handleUserAuthenticationException(UserAuthenticationException exception) {
        log.error("Authentication error: {}", exception.getMessage(), exception);
        ExceptionResponse response = new ExceptionResponse(
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.error("Access denied: {}", exception.getMessage(), exception);
        ExceptionResponse response = new ExceptionResponse(
                exception.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsException(BadCredentialsException exception) {
        log.error("Bad credentials: {}", exception.getMessage(), exception);
        ExceptionResponse response = new ExceptionResponse(
                "Geçersiz kullanıcı adı veya şifre",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException exception) {
        log.error("Validation error: {}", exception.getMessage(), exception);

        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Geçersiz değer"
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        log.error("Constraint violation: {}", exception.getMessage(), exception);

        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(", "));

        ExceptionResponse response = new ExceptionResponse(
                message,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ExceptionResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.error("File size exceeded: {}", exception.getMessage(), exception);

        ExceptionResponse response = new ExceptionResponse(
                "Dosya boyutu çok büyük. Maksimum 10MB yükleyebilirsiniz.",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception exception) {
        log.error("Unexpected error occurred: {}", exception.getMessage(), exception);

        ExceptionResponse response = new ExceptionResponse(
                "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * Handle ValidationException
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ApiError error = new ApiError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle OrderCreationException
     * HTTP Status: 500 Internal Server Error
     */
    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ApiError> handleOrderCreationException(OrderCreationException ex) {
        log.error("Order creation error: {}", ex.getMessage(), ex);
        ApiError error = new ApiError("Sipariş oluşturulamadı. Lütfen tekrar deneyin.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle InsufficientStockException
     * HTTP Status: 409 Conflict
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<StockErrorResponse> handleInsufficientStockException(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());

        StockErrorResponse error = StockErrorResponse.builder()
                .message(ex.getMessage())
                .productName(ex.getProductName())
                .availableStock(ex.getAvailableStock())
                .requestedQuantity(ex.getRequestedQuantity())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle PaymentProcessingException
     * HTTP Status: 402 Payment Required
     * Iyzico Integration
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentProcessingException(PaymentProcessingException ex) {
        log.error("Payment processing error: {} (Code: {}, OrderId: {}, PaymentId: {})",
                ex.getMessage(), ex.getErrorCode(), ex.getOrderId(), ex.getPaymentId());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.PAYMENT_REQUIRED.value());
        response.put("error", "Payment Required");
        response.put("message", ex.getMessage());
        response.put("errorCode", ex.getErrorCode());

        if (ex.getOrderId() != null) {
            response.put("orderId", ex.getOrderId());
        }
        if (ex.getPaymentId() != null) {
            response.put("paymentId", ex.getPaymentId());
        }

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }
}