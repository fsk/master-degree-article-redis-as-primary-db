package com.fsk.springbootredis.dto.mapper;

import com.fsk.springbootredis.dto.responses.OrderItemResponse;
import com.fsk.springbootredis.dto.responses.OrderResponse;
import com.fsk.springbootredis.entity.OrderEntity;
import com.fsk.springbootredis.entity.OrderItemEntity;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "totalAmount", expression = "java(calculateTotal(order))")
    OrderResponse toResponse(OrderEntity order);

    @Mapping(target = "productId", source = "productId")
    OrderItemResponse toItemResponse(OrderItemEntity item);

    default BigDecimal calculateTotal(OrderEntity order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
