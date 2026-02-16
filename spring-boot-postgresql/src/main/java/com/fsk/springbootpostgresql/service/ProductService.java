package com.fsk.springbootpostgresql.service;

import com.fsk.springbootpostgresql.dto.mapper.ProductMapper;
import com.fsk.springbootpostgresql.dto.requests.CreateProductRequest;
import com.fsk.springbootpostgresql.dto.requests.UpdateStockRequest;
import com.fsk.springbootpostgresql.entity.ProductEntity;
import com.fsk.springbootpostgresql.exception.ProductNotFoundException;
import com.fsk.springbootpostgresql.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductEntity createProduct(CreateProductRequest request) {
        return productRepository.save(productMapper.toEntity(request));
    }

    public ProductEntity getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    @Transactional
    public ProductEntity updateStock(UUID productId, UpdateStockRequest request) {
        ProductEntity product = getProduct(productId);
        product.applyStockDelta(request.delta());
        return product;
    }
}
