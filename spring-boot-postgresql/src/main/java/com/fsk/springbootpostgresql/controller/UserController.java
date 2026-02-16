package com.fsk.springbootpostgresql.controller;

import com.fsk.springbootpostgresql.api.ApiResponse;
import com.fsk.springbootpostgresql.dto.requests.CreateUserRequest;
import com.fsk.springbootpostgresql.entity.UserEntity;
import com.fsk.springbootpostgresql.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserEntity>> create(@RequestBody CreateUserRequest request) {
        UserEntity user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user));
    }
}
