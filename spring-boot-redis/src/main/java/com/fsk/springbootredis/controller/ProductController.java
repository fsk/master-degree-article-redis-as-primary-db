package com.fsk.springbootredis.controller;

import com.fsk.springbootredis.api.ApiResponse;
import com.fsk.springbootredis.dto.requests.CreateProductRequest;
import com.fsk.springbootredis.dto.requests.UpdateStockRequest;
import com.fsk.springbootredis.entity.ProductEntity;
import com.fsk.springbootredis.service.ProductService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductEntity>> create(@RequestBody CreateProductRequest request) {
        ProductEntity product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductEntity>> get(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
    }

    @PostMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<ProductEntity>> updateStock(
            @PathVariable UUID productId,
            @RequestBody UpdateStockRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateStock(productId, request)));
    }
}
