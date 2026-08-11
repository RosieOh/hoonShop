package com.hoonshop.common.config;

import com.hoonshop.catalog.domain.*;
import com.hoonshop.common.domain.Money;
import com.hoonshop.identity.domain.Email;
import com.hoonshop.identity.domain.PasswordEncoder;
import com.hoonshop.identity.domain.Role;
import com.hoonshop.identity.domain.User;
import com.hoonshop.identity.domain.UserRepository;
import com.hoonshop.promotion.domain.Coupon;
import com.hoonshop.promotion.domain.CouponRepository;
import com.hoonshop.promotion.domain.DiscountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * 개발용 시드 데이터.
 *
 * <p>SQL 대신 Java로 넣는 이유가 두 가지 있습니다.
 * <ol>
 *   <li>비밀번호를 BCrypt로 해싱해야 하는데 SQL로는 할 수 없습니다.</li>
 *   <li>도메인 팩토리를 거치므로 시드 데이터도 불변식 검증을 받습니다 —
 *       SQL로 직접 넣으면 도메인이 거부할 데이터가 DB에 들어갈 수 있습니다.</li>
 * </ol>
 *
 * <p>이미 데이터가 있으면 아무것도 하지 않습니다. 운영에서는
 * {@code hoonshop.seed.enabled=false}로 꺼두세요.
 */
@Component
@ConditionalOnProperty(name = "hoonshop.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEMO_PASSWORD = "hoonshop";

    /** 컬러웨이별 4단계 팔레트 (프론트의 BeadArt가 이 값으로 상품을 그립니다) */
    private static final String[][] PALETTES = {
            {"butter", "#F6D89B", "#FFF2D6", "#E8B96A", "#FBE7BC"},
            {"peach", "#F5B5A3", "#FFE1D6", "#E08C74", "#FCD3C6"},
            {"mint", "#A8D8C8", "#DFF3EC", "#6FBBA4", "#C8E9DE"},
            {"lilac", "#C4B0DE", "#EBE2F6", "#9C82C4", "#DCCFEE"},
            {"ocean", "#9CC3E0", "#DCEBF7", "#6296C0", "#C3DCEE"},
            {"cherry", "#E58BA0", "#FBD7DF", "#C75F79", "#F2BCC8"},
            {"cocoa", "#C6A488", "#EEDFD1", "#9C7A5B", "#E0CBB6"},
            {"pearl", "#F1EAE2", "#FFFFFF", "#D9CCBE", "#FAF5EF"},
            {"olive", "#B9C79A", "#E5EBD5", "#8FA070", "#D3DCBE"},
            {"ink", "#6B6E8C", "#C9CBDD", "#474A66", "#A6A9C2"},
    };

    /** {id, 라벨, hex} — 컬러 옵션 스와치 */
    private static final String[][] SWATCHES = {
            {"butter", "버터크림", "#F6D89B"},
            {"peach", "피치", "#F5B5A3"},
            {"mint", "민트", "#A8D8C8"},
            {"lilac", "라일락", "#C4B0DE"},
            {"ocean", "오션", "#9CC3E0"},
            {"cherry", "체리", "#E58BA0"},
            {"cocoa", "코코아", "#C6A488"},
            {"pearl", "펄화이트", "#F1EAE2"},
            {"olive", "올리브", "#B9C79A"},
            {"ink", "잉크블루", "#6B6E8C"},
    };

    /** {이름, 카테고리, 컬러웨이, 정가, 할인율, 평점, 리뷰수, 배지, 설명} */
    private static final Object[][] PRODUCTS = {
            {"오후 세 시의 버터", "necklace", "butter", 32_000L, 15, 4.8, 214, "BEST",
                    "체코 파이어폴리시 비즈에 14K 골드필드 클래스프. 햇빛 각도에 따라 결이 달라집니다."},
            {"진주 한 알만", "necklace", "pearl", 46_000L, 0, 4.9, 331, "BEST",
                    "담수 바로크 진주 한 알을 실버 체인 정중앙에 물렸습니다. 어떤 옷에도 방해되지 않는 크기."},
            {"라일락 스몰토크", "necklace", "lilac", 29_000L, 0, 4.6, 87, "NEW",
                    "4mm 라일락 유리 비즈를 촘촘히. 목선을 짧게 감싸는 초커 길이입니다."},
            {"체리 콤포트", "necklace", "cherry", 35_000L, 20, 4.7, 152, "",
                    "체리색 라운드 비즈와 골드 시드 비즈를 번갈아 꿰어 리듬감을 만들었습니다."},
            {"미드나잇 링크", "necklace", "ink", 52_000L, 0, 4.8, 64, "LIMITED",
                    "잉크블루 큐브 비즈 한정 수량. 재입고 계획이 없는 컬러입니다."},
            {"민트 소다 팔찌", "bracelet", "mint", 19_000L, 0, 4.7, 402, "BEST",
                    "탄성 실 사용으로 잠금장치 없이 착용. 여름에 가장 많이 나가는 스테디셀러."},
            {"피치 마카롱", "bracelet", "peach", 22_000L, 10, 4.5, 188, "",
                    "무광 아크릴 비즈라 부딪혀도 소리가 나지 않습니다. 사무실용으로 추천."},
            {"코코아 위빙", "bracelet", "cocoa", 27_000L, 0, 4.6, 96, "NEW",
                    "두 줄을 엮어 짠 위빙 팔찌. 손목이 얇아도 흘러내리지 않습니다."},
            {"오션 드립", "bracelet", "ocean", 24_000L, 0, 4.4, 73, "",
                    "투명 비즈 사이로 빛이 통과하며 물방울처럼 보입니다."},
            {"데이지 체인", "bracelet", "olive", 21_000L, 15, 4.8, 245, "BEST",
                    "시드 비즈로 한 송이씩 데이지를 엮었습니다. 한 줄 완성에 40분."},
            {"버터 드롭 이어링", "earring", "butter", 26_000L, 0, 4.9, 176, "BEST",
                    "침 부분은 순은. 귀 예민한 분들 후기가 특히 좋습니다."},
            {"펄 미니 후프", "earring", "pearl", 31_000L, 0, 4.8, 289, "",
                    "2cm 후프에 미니 진주 5알. 가벼워서 종일 착용해도 늘어지지 않습니다."},
            {"라일락 캔디", "earring", "lilac", 23_000L, 20, 4.5, 61, "NEW",
                    "사탕처럼 둥근 비즈 두 알. 캐주얼한 데일리 룩에."},
            {"체리 롱 스트랜드", "earring", "cherry", 34_000L, 0, 4.6, 44, "LIMITED",
                    "6cm 롱 드롭. 얼굴선을 길어 보이게 합니다."},
            {"시드 비즈 링 세트", "ring", "mint", 15_000L, 0, 4.7, 358, "BEST",
                    "3개 한 세트. 겹쳐 끼거나 따로 끼거나."},
            {"버터 큐브 링", "ring", "butter", 13_000L, 10, 4.4, 129, "",
                    "사각 비즈 하나만 올린 미니멀 링."},
            {"오션 트위스트", "ring", "ocean", 17_000L, 0, 4.5, 82, "NEW",
                    "두 겹으로 꼬아 만든 반지. 프리 사이즈."},
            {"모래알 발찌", "anklet", "pearl", 18_000L, 0, 4.6, 141, "",
                    "발목에서 잘게 부서지는 빛. 방수 코팅 처리."},
            {"올리브 리프 발찌", "anklet", "olive", 20_000L, 15, 4.5, 67, "NEW",
                    "잎사귀 참을 하나 달았습니다. 길이 조절 3단계."},
            {"비즈 폰 스트랩", "strap", "cherry", 25_000L, 0, 4.8, 512, "BEST",
                    "손목 통과 사이즈. 무게 12g으로 폰 흔들림이 적습니다."},
            {"버터 키링", "strap", "butter", 16_000L, 0, 4.6, 203, "",
                    "가방에 달면 끝. 링 부분은 무니켈 도금."},
            {"민트 백 참", "strap", "mint", 19_000L, 25, 4.4, 88, "",
                    "시즌 오프 할인 중. 재고 소진 시 종료됩니다."},
            {"잉크 카드홀더 스트랩", "strap", "ink", 28_000L, 0, 4.7, 55, "NEW",
                    "사원증·카드홀더용 넥스트랩. 안전 버클 포함."},
            {"코코아 에어팟 참", "strap", "cocoa", 17_000L, 0, 4.5, 119, "",
                    "에어팟 케이스 고리에 딱 맞는 사이즈."},
    };

    private static final int[] STOCK_PATTERN = {3, 48, 120, 7, 0, 64, 25};

    private final ProductRepository products;
    private final InventoryRepository inventories;
    private final UserRepository users;
    private final CouponRepository coupons;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(ProductRepository products, InventoryRepository inventories,
                      UserRepository users, CouponRepository coupons,
                      PasswordEncoder passwordEncoder) {
        this.products = products;
        this.inventories = inventories;
        this.users = users;
        this.coupons = coupons;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0) {
            log.info("시드 데이터가 이미 있습니다. 건너뜁니다.");
            return;
        }

        seedUsers();
        seedProducts();
        seedCoupons();
        log.info("시드 데이터 생성 완료 — 상품 {}종", PRODUCTS.length);
    }

    private void seedUsers() {
        users.save(User.restore(Email.of("hoon@example.com"), DEMO_PASSWORD, "김태훈",
                Role.CUSTOMER, "GOLD", 3200, Instant.parse("2025-11-02T00:00:00Z"),
                passwordEncoder));

        users.save(User.restore(Email.of("admin@hoonshop.com"), DEMO_PASSWORD, "김태훈",
                Role.ADMIN, "STAFF", 0, Instant.parse("2025-09-01T00:00:00Z"), passwordEncoder));
    }

    private void seedProducts() {
        Instant now = Instant.now();

        for (int i = 0; i < PRODUCTS.length; i++) {
            Object[] row = PRODUCTS[i];
            String name = (String) row[0];
            Category category = Category.fromCode((String) row[1]);
            String colorway = (String) row[2];
            long listPrice = (Long) row[3];
            int discountRate = (Integer) row[4];
            double rating = (Double) row[5];
            int reviewCount = (Integer) row[6];
            String badge = (String) row[7];
            String description = (String) row[8];

            Product product = Product.create(
                            ProductCode.ofSequence(i + 1), name, category, colorway, description,
                            Money.won(listPrice), discountRate,
                            now.minus(i * 3L, ChronoUnit.DAYS))
                    .withPalette(paletteOf(colorway))
                    .withMaterials(materialsFor(category))
                    .withSizes(sizesFor(category))
                    .withColorOptions(colorOptionsFor(colorway))
                    .withBadges(badge.isEmpty() ? Set.of() : Set.of(Badge.valueOf(badge)))
                    .withReputation(rating, reviewCount, 1200 - i * 37);

            products.save(product);
            inventories.save(Inventory.of(product.code(), STOCK_PATTERN[i % STOCK_PATTERN.length]));
        }
    }

    private List<String> paletteOf(String colorway) {
        for (String[] row : PALETTES) {
            if (row[0].equals(colorway)) {
                return List.of(row[1], row[2], row[3], row[4]);
            }
        }
        return List.of("#E8DFD4", "#F7F2EA", "#D3C5B5", "#FBF8F4");
    }

    /** 기본 컬러 + 인접 두 가지를 옵션으로 제공합니다. */
    private List<ColorOption> colorOptionsFor(String colorway) {
        int base = 0;
        for (int i = 0; i < SWATCHES.length; i++) {
            if (SWATCHES[i][0].equals(colorway)) {
                base = i;
                break;
            }
        }
        int finalBase = base;
        return List.of(0, 3, 6).stream()
                .map(offset -> SWATCHES[(finalBase + offset) % SWATCHES.length])
                .map(s -> ColorOption.of(s[0], s[1], s[2]))
                .toList();
    }

    private List<String> materialsFor(Category category) {
        return category == Category.EARRING
                ? List.of("체코 유리 비즈", "925 실버 침", "골드필드 마감")
                : List.of("체코 유리 비즈", "스테인리스 와이어", "무니켈 도금 부자재");
    }

    private List<String> sizesFor(Category category) {
        return switch (category) {
            case NECKLACE -> List.of("38cm (초커)", "42cm (기본)", "45cm (레이어드)");
            case BRACELET -> List.of("15cm (S)", "16.5cm (M)", "18cm (L)");
            case ANKLET -> List.of("22cm", "24cm", "26cm");
            case RING -> List.of("9호", "11호", "13호", "15호");
            case EARRING, STRAP -> List.of();
        };
    }

    private void seedCoupons() {
        Instant now = Instant.now();

        coupons.save(Coupon.create("CPN-WELCOME", "신규 가입 15% 쿠폰", DiscountType.PERCENT, 15,
                Money.won(20_000), Money.won(8_000), now.plus(180, ChronoUnit.DAYS), false));

        coupons.save(Coupon.create("CPN-SUMMER", "여름맞이 5,000원 할인", DiscountType.AMOUNT, 5_000,
                Money.won(40_000), Money.won(5_000), now.plus(60, ChronoUnit.DAYS), true));

        coupons.save(Coupon.create("CPN-SHIP", "배송비 무료 쿠폰", DiscountType.SHIPPING, 3_000,
                Money.ZERO, Money.won(3_000), now.plus(30, ChronoUnit.DAYS), true));

        // 만료 쿠폰도 하나 넣어둡니다 — "왜 못 쓰는지" 화면을 실제로 볼 수 있어야 합니다.
        coupons.save(Coupon.create("CPN-EXPIRED", "봄 시즌 10% 쿠폰", DiscountType.PERCENT, 10,
                Money.won(10_000), Money.won(5_000), now.minus(30, ChronoUnit.DAYS), false));
    }
}
