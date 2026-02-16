package com.fsk.springbootredis.service;

import com.fsk.springbootredis.dto.mapper.ProductMapper;
import com.fsk.springbootredis.dto.requests.CreateProductRequest;
import com.fsk.springbootredis.dto.requests.UpdateStockRequest;
import com.fsk.springbootredis.entity.ProductEntity;
import com.fsk.springbootredis.exception.ProductNotFoundException;
import com.fsk.springbootredis.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductEntity createProduct(CreateProductRequest request) {
        ProductEntity product = productMapper.toEntity(request);
        product.setId(UUID.randomUUID());
        return productRepository.save(product);
    }

    public ProductEntity getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    public ProductEntity updateStock(UUID productId, UpdateStockRequest request) {
        ProductEntity product = getProduct(productId);
        product.applyStockDelta(request.delta());
        return productRepository.save(product);
    }
}
