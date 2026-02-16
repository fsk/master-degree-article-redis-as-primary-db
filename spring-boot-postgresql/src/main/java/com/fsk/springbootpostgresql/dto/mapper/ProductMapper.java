package com.fsk.springbootpostgresql.dto.mapper;

import com.fsk.springbootpostgresql.dto.requests.CreateProductRequest;
import com.fsk.springbootpostgresql.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    ProductEntity toEntity(CreateProductRequest request);
}
