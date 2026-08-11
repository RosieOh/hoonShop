package com.hoonshop.identity.domain;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider,
                                                            String providerUserId);

    List<SocialAccount> findByUserId(Long userId);

    SocialAccount save(SocialAccount account);
}
