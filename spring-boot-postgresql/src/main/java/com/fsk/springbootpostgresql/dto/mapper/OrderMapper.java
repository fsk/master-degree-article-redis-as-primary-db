package com.fsk.springbootpostgresql.dto.mapper;

import com.fsk.springbootpostgresql.dto.responses.OrderItemResponse;
import com.fsk.springbootpostgresql.dto.responses.OrderResponse;
import com.fsk.springbootpostgresql.entity.OrderEntity;
import com.fsk.springbootpostgresql.entity.OrderItemEntity;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "totalAmount", expression = "java(calculateTotal(order))")
    OrderResponse toResponse(OrderEntity order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItemEntity item);

    default BigDecimal calculateTotal(OrderEntity order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
