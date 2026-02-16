package com.fsk.springbootredis.dto.mapper;

import com.fsk.springbootredis.dto.requests.CreateProductRequest;
import com.fsk.springbootredis.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    ProductEntity toEntity(CreateProductRequest request);
}
