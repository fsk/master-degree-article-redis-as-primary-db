package com.fsk.springbootpostgresql.exception;

import com.fsk.springbootpostgresql.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UserNotFoundException.class,
            ProductNotFoundException.class,
            OrderNotFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ApiException ex
    ) {
        return buildError(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            EmptyOrderItemsException.class,
            InvalidOrderQuantityException.class,
            InsufficientStockException.class,
            StockBelowZeroException.class,
            ForcedRollbackException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            ApiException ex
    ) {
        return buildError(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeadlockDetectedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDeadlock(ApiException ex) {
        return buildError(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        currentPath()
                ));
    }

    private ResponseEntity<ApiResponse<Void>> buildError(
            ApiException exception,
            HttpStatus status
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(
                        exception.getCode(),
                        exception.getMessage(),
                        status.value(),
                        currentPath()
                ));
    }

    private String currentPath() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
    }
}
