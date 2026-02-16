package com.fsk.springbootpostgresql.repository;

import com.fsk.springbootpostgresql.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}
