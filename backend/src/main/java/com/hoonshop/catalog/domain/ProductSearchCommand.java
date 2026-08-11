package com.hoonshop.catalog.domain;

import com.hoonshop.common.domain.Money;
import java.util.List;

/**
 * 상품 검색 조건.
 *
 * <p>컨트롤러의 쿼리 파라미터를 그대로 저장소까지 흘려보내지 않고 도메인 언어로 한 번
 * 번역합니다. "size=12" 같은 HTTP 관례가 도메인에 스며들지 않게 하려는 것입니다.
 */
public record ProductSearchCommand(
        Category category,
        String keyword,
        Money minPrice,
        Money maxPrice,
        List<String> colorIds,
        Badge badge,
        SortBy sortBy,
        int page,
        int size
) {

    public ProductSearchCommand {
        if (page < 1) {
            throw new IllegalArgumentException("페이지는 1부터 시작합니다: " + page);
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1~100 사이여야 합니다: " + size);
        }
        colorIds = colorIds == null ? List.of() : List.copyOf(colorIds);
    }

    public int offset() {
        return (page - 1) * size;
    }

    public enum SortBy {
        RECOMMEND("recommend"),
        NEW("new"),
        POPULAR("popular"),
        PRICE_ASC("price_asc"),
        PRICE_DESC("price_desc"),
        REVIEW("review");

        private final String code;

        SortBy(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static SortBy fromCode(String code) {
            if (code == null || code.isBlank()) {
                return RECOMMEND;
            }
            for (SortBy value : values()) {
                if (value.code.equalsIgnoreCase(code)) {
                    return value;
                }
            }
            return RECOMMEND;
        }
    }
}
