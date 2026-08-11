package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** Spring Data 어댑터. 도메인은 이 인터페이스를 모릅니다. */
public interface ProductJpaRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCodeValue(String code);

    List<Product> findByCodeValueIn(List<String> codes);
}
