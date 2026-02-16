package com.fsk.springbootredis.repository;

import com.fsk.springbootredis.entity.ProductEntity;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<ProductEntity, UUID> {
}
