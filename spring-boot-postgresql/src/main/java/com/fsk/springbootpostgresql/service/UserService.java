package com.fsk.springbootpostgresql.service;

import com.fsk.springbootpostgresql.dto.mapper.UserMapper;
import com.fsk.springbootpostgresql.dto.requests.CreateUserRequest;
import com.fsk.springbootpostgresql.entity.UserEntity;
import com.fsk.springbootpostgresql.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserEntity createUser(CreateUserRequest request) {
        return userRepository.save(userMapper.toEntity(request));
    }
}
