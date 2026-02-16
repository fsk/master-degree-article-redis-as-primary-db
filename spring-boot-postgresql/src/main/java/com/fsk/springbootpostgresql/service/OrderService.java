package com.fsk.springbootpostgresql.service;

import com.fsk.springbootpostgresql.dto.mapper.OrderMapper;
import com.fsk.springbootpostgresql.dto.requests.PlaceOrderRequest;
import com.fsk.springbootpostgresql.dto.responses.OrderResponse;
import com.fsk.springbootpostgresql.entity.OrderEntity;
import com.fsk.springbootpostgresql.entity.OrderStatus;
import com.fsk.springbootpostgresql.entity.ProductEntity;
import com.fsk.springbootpostgresql.entity.UserEntity;
import com.fsk.springbootpostgresql.exception.OrderNotFoundException;
import com.fsk.springbootpostgresql.exception.ProductNotFoundException;
import com.fsk.springbootpostgresql.exception.UserNotFoundException;
import com.fsk.springbootpostgresql.repository.OrderRepository;
import com.fsk.springbootpostgresql.repository.ProductRepository;
import com.fsk.springbootpostgresql.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        request.validateItems();

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(UserNotFoundException::new);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.CREATED);

        List<UUID> productIds = request.items().stream()
                .map(item -> item.productId())
                .distinct()
                .toList();

        List<ProductEntity> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ProductNotFoundException();
        }

        var productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, product -> product));

        for (var item : request.items()) {
            ProductEntity product = productMap.get(item.productId());
            if (product == null) {
                throw new ProductNotFoundException();
            }
            order.addItem(product, item.quantity());
        }

        OrderEntity saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        return orderMapper.toResponse(order);
    }
}
