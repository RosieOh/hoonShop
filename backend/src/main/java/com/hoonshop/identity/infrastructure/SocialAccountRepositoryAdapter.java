package com.hoonshop.identity.infrastructure;

import com.hoonshop.identity.domain.SocialAccount;
import com.hoonshop.identity.domain.SocialAccountRepository;
import com.hoonshop.identity.domain.SocialProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SocialAccountRepositoryAdapter implements SocialAccountRepository {

    private final SocialAccountJpaRepository jpa;

    public SocialAccountRepositoryAdapter(SocialAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider,
                                                                   String providerUserId) {
        return jpa.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<SocialAccount> findByUserId(Long userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public SocialAccount save(SocialAccount account) {
        return jpa.save(account);
    }
}
