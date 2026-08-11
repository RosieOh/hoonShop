package com.hoonshop.identity.infrastructure;

import com.hoonshop.identity.domain.SocialAccount;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider,
                                                            String providerUserId);

    List<SocialAccount> findByUserId(Long userId);
}
