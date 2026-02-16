package com.fsk.springbootredis.service;

import com.fsk.springbootredis.dto.mapper.UserMapper;
import com.fsk.springbootredis.dto.requests.CreateUserRequest;
import com.fsk.springbootredis.entity.UserEntity;
import com.fsk.springbootredis.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserEntity createUser(CreateUserRequest request) {
        UserEntity user = userMapper.toEntity(request);
        user.setId(UUID.randomUUID());
        return userRepository.save(user);
    }
}
