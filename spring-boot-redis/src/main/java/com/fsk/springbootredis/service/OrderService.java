package com.fsk.springbootredis.service;

import com.fsk.springbootredis.dto.mapper.OrderMapper;
import com.fsk.springbootredis.dto.requests.PlaceOrderRequest;
import com.fsk.springbootredis.dto.responses.OrderResponse;
import com.fsk.springbootredis.entity.OrderEntity;
import com.fsk.springbootredis.entity.OrderStatus;
import com.fsk.springbootredis.entity.ProductEntity;
import com.fsk.springbootredis.entity.UserEntity;
import com.fsk.springbootredis.exception.OrderNotFoundException;
import com.fsk.springbootredis.exception.ProductNotFoundException;
import com.fsk.springbootredis.exception.UserNotFoundException;
import com.fsk.springbootredis.repository.OrderRepository;
import com.fsk.springbootredis.repository.ProductRepository;
import com.fsk.springbootredis.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderResponse placeOrder(PlaceOrderRequest request) {
        request.validateItems();

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(UserNotFoundException::new);

        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setUserId(user.getId());
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.CREATED);

        List<UUID> productIds = request.items().stream()
                .map(item -> item.productId())
                .distinct()
                .toList();

        List<ProductEntity> products = StreamSupport.stream(
                        productRepository.findAllById(productIds).spliterator(),
                        false)
                .toList();
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

        productRepository.saveAll(products);
        OrderEntity saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    public OrderResponse getOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        return orderMapper.toResponse(order);
    }
}
