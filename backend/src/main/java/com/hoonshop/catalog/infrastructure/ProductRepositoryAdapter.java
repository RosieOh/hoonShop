package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hoonshop.catalog.infrastructure.ProductSpecifications.*;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final EntityManager entityManager;

    public ProductRepositoryAdapter(ProductJpaRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Product> findByCode(ProductCode code) {
        return jpa.findByCodeValue(code.value());
    }

    @Override
    public List<Product> findAllByCodes(List<ProductCode> codes) {
        return jpa.findByCodeValueIn(codes.stream().map(ProductCode::value).toList());
    }

    @Override
    public ProductSearchResult search(ProductSearchCommand command) {
        Specification<Product> spec = Specification.allOf(
                hasCategory(command.category()),
                matchesKeyword(command.keyword()),
                priceAtLeast(command.minPrice()),
                priceAtMost(command.maxPrice()),
                hasAnyColor(command.colorIds()),
                hasBadge(command.badge()));

        Pageable pageable = PageRequest.of(command.page() - 1, command.size());

        // 정렬 기준 중 판매가·추천순은 컬럼이 아니라 식이라 Sort로 표현할 수 없어
        // Criteria로 직접 조립합니다.
        Page<Product> page = switch (command.sortBy()) {
            case PRICE_ASC, PRICE_DESC, RECOMMEND -> searchWithExpressionOrder(spec, command, pageable);
            case NEW -> jpa.findAll(spec, PageRequest.of(command.page() - 1, command.size(),
                    Sort.by(Sort.Direction.DESC, "createdAt")));
            case POPULAR -> jpa.findAll(spec, PageRequest.of(command.page() - 1, command.size(),
                    Sort.by(Sort.Direction.DESC, "soldCount")));
            case REVIEW -> jpa.findAll(spec, PageRequest.of(command.page() - 1, command.size(),
                    Sort.by(Sort.Direction.DESC, "reviewCount")));
        };

        return new ProductSearchResult(page.getContent(), command.page(), command.size(),
                page.getTotalElements());
    }

    private Page<Product> searchWithExpressionOrder(Specification<Product> spec,
                                                    ProductSearchCommand command,
                                                    Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> root = query.from(Product.class);

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        List<Order> orders = new ArrayList<>();
        switch (command.sortBy()) {
            case PRICE_ASC -> orders.add(cb.asc(sellingPriceExpression(cb, root)));
            case PRICE_DESC -> orders.add(cb.desc(sellingPriceExpression(cb, root)));
            default -> {
                // 추천순: 평점이 높고 리뷰가 많은 순. 리뷰 3개짜리 만점 상품이
                // 리뷰 300개짜리 4.8점 상품보다 앞서는 걸 막습니다.
                orders.add(cb.desc(root.get("rating")));
                orders.add(cb.desc(root.get("reviewCount")));
            }
        }
        orders.add(cb.asc(root.get("id"))); // 동점일 때 페이지 간 순서가 흔들리지 않도록
        query.orderBy(orders);

        List<Product> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable, jpa.count(spec));
    }

    @Override
    public List<Product> findRelated(Category category, ProductCode exclude, int limit) {
        Specification<Product> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("category"), category),
                cb.notEqual(root.get("code").get("value"), exclude.value()));
        return jpa.findAll(spec, PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "soldCount"))).getContent();
    }

    @Override
    public Product save(Product product) {
        return jpa.save(product);
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
