package com.fsk.springbootpostgresql.dto.mapper;

import com.fsk.springbootpostgresql.dto.requests.CreateUserRequest;
import com.fsk.springbootpostgresql.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    UserEntity toEntity(CreateUserRequest request);
}
