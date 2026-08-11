package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.domain.Badge;
import com.hoonshop.catalog.domain.Category;
import com.hoonshop.catalog.domain.Product;
import com.hoonshop.common.domain.Money;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 상품 검색 조건 조립.
 *
 * <p>조건이 없는 필터는 명세를 만들지 않고 넘어갑니다 — {@code WHERE 1=1} 뒤에
 * 조건을 문자열로 이어붙이는 방식과 달리 인덱스가 살아있고 SQL 인젝션 여지가 없습니다.
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * 판매가 표현식: {@code 정가 × (100 - 할인율) / 100}.
     *
     * <p>도메인의 {@code sellingPrice()}는 100원 단위로 반올림하지만 여기서는 하지 않습니다.
     * 정렬·구간 필터에서 100원 미만 차이는 결과 순서를 바꾸지 않고, 반올림을 SQL로 옮기면
     * 인덱스를 못 타는 표현식이 하나 더 늘어납니다. <b>표시·청구 금액은 항상 도메인 값을 씁니다.</b>
     */
    static Expression<Number> sellingPriceExpression(CriteriaBuilder cb, Root<Product> root) {
        Expression<Long> listPrice = root.get("listPrice").get("amount");
        Expression<Integer> rate = root.get("discountRate");
        return cb.quot(cb.prod(listPrice, cb.diff(cb.literal(100), rate)), cb.literal(100));
    }

    static Specification<Product> hasCategory(Category category) {
        return category == null ? null : (root, q, cb) -> cb.equal(root.get("category"), category);
    }

    static Specification<Product> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    static Specification<Product> priceAtLeast(Money min) {
        return min == null ? null : (root, q, cb) ->
                cb.ge(sellingPriceExpression(cb, root), min.value());
    }

    static Specification<Product> priceAtMost(Money max) {
        return max == null ? null : (root, q, cb) ->
                cb.le(sellingPriceExpression(cb, root), max.value());
    }

    static Specification<Product> hasAnyColor(List<String> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            // 컬렉션 조인은 행을 늘리므로 중복 제거가 필요합니다.
            if (query != null) {
                query.distinct(true);
            }
            Join<Object, Object> join = root.join("colorOptions");
            return join.get("id").in(colorIds);
        };
    }

    static Specification<Product> hasBadge(Badge badge) {
        if (badge == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.equal(root.join("badges"), badge);
        };
    }
}
