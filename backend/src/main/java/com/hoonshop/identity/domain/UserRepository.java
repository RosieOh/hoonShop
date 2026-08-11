package com.hoonshop.identity.domain;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);
}
