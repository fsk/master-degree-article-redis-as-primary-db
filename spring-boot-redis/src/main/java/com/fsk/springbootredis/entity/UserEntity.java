package com.fsk.springbootredis.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@RedisHash("users")
public class UserEntity implements Serializable {
    @Id
    private UUID id;
    private String name;
    private String email;
}
