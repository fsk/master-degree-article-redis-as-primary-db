package com.fsk.springbootredis.repository;

import com.fsk.springbootredis.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<OrderEntity, UUID> {
}
