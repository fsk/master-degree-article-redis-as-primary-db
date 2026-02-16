package com.fsk.springbootpostgresql.repository;

import com.fsk.springbootpostgresql.entity.UserEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
}
