package com.fsk.springbootredis.dto.mapper;

import com.fsk.springbootredis.dto.requests.CreateUserRequest;
import com.fsk.springbootredis.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    UserEntity toEntity(CreateUserRequest request);
}
