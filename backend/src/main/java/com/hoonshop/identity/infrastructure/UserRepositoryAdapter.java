package com.hoonshop.identity.infrastructure;

import com.hoonshop.identity.domain.Email;
import com.hoonshop.identity.domain.User;
import com.hoonshop.identity.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmailValue(email.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmailValue(email.value());
    }

    @Override
    public User save(User user) {
        return jpa.save(user);
    }
}
