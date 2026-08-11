package com.hoonshop.identity.infrastructure;

import com.hoonshop.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailValue(String email);

    boolean existsByEmailValue(String email);
}
