package com.hoonshop.identity.domain;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);
}
