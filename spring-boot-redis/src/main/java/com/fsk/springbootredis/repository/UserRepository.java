package com.fsk.springbootredis.repository;

import com.fsk.springbootredis.entity.UserEntity;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserEntity, UUID> {
}
