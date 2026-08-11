package com.hoonshop.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * 상품 저장소 포트.
 *
 * <p>인터페이스가 도메인에 있고 구현이 infrastructure에 있습니다. 도메인이 JPA를 향하지 않고
 * JPA가 도메인을 향하게 되어(의존성 역전), 나중에 조회를 캐시나 검색엔진으로 바꿔도
 * 도메인 코드는 그대로입니다.
 */
public interface ProductRepository {

    Optional<Product> findByCode(ProductCode code);

    List<Product> findAllByCodes(List<ProductCode> codes);

    ProductSearchResult search(ProductSearchCommand command);

    List<Product> findRelated(Category category, ProductCode exclude, int limit);

    Product save(Product product);

    long count();
}
