package com.hoonshop.admin.application;

import com.hoonshop.catalog.application.InventoryService;
import com.hoonshop.order.domain.Order;
import com.hoonshop.order.domain.OrderLine;
import com.hoonshop.order.domain.OrderRepository;
import com.hoonshop.order.domain.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 관리자 대시보드 집계.
 *
 * <p>이 서비스는 여러 컨텍스트를 가로질러 읽습니다. 그래서 도메인 컨텍스트 안에 두지 않고
 * 별도 {@code admin} 모듈에 뒀습니다 — <b>리포팅은 쓰기 모델과 성격이 다릅니다.</b>
 * 애그리거트 경계를 지키느라 통계를 여러 번 왕복해서 구하는 것보다, 읽기 전용 모듈이
 * 여러 컨텍스트를 조회하는 편이 정직합니다.
 *
 * <p>규모가 커지면 이 부분이 가장 먼저 아픕니다. 그때는 집계 테이블이나 읽기 전용
 * 복제본으로 분리하세요(CQRS). 지금은 주문 수백 건이라 실시간 집계로 충분합니다.
 */
@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int TREND_DAYS = 14;

    private final OrderRepository orders;
    private final InventoryService inventoryService;

    public AdminStatsService(OrderRepository orders, InventoryService inventoryService) {
        this.orders = orders;
        this.inventoryService = inventoryService;
    }

    public Stats stats() {
        List<Order> all = orders.searchForAdmin(null, null);
        List<Order> valid = all.stream()
                .filter(o -> o.status() != OrderStatus.CANCELLED)
                .toList();

        LocalDate today = LocalDate.now(SEOUL);
        List<Order> todayOrders = valid.stream()
                .filter(o -> toDate(o.createdAt()).equals(today))
                .toList();

        long revenueTotal = valid.stream().mapToLong(o -> o.amounts().payable().value()).sum();

        // 최근 14일: 주문이 없는 날도 0으로 채워야 차트의 x축이 끊기지 않습니다.
        Map<LocalDate, DailyAccumulator> byDate = new HashMap<>();
        valid.forEach(o -> byDate
                .computeIfAbsent(toDate(o.createdAt()), d -> new DailyAccumulator())
                .add(o.amounts().payable().value()));

        List<DailyRevenue> dailyRevenue = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailyAccumulator acc = byDate.getOrDefault(date, new DailyAccumulator());
            dailyRevenue.add(new DailyRevenue(date.toString(), acc.revenue, acc.count));
        }

        List<StatusCount> statusCounts = Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.PAYMENT_PENDING)
                .map(s -> new StatusCount(s.name(), s.label(),
                        all.stream().filter(o -> o.status() == s).count()))
                .toList();

        List<InventoryService.LowStockItem> lowStock = inventoryService.lowStock();

        return new Stats(
                todayOrders.stream().mapToLong(o -> o.amounts().payable().value()).sum(),
                todayOrders.size(),
                revenueTotal,
                valid.size(),
                valid.isEmpty() ? 0 : revenueTotal / valid.size(),
                all.stream().filter(o -> o.status() == OrderStatus.PAID
                        || o.status() == OrderStatus.MAKING).count(),
                lowStock.size(),
                lowStock.stream().filter(i -> i.stock() == 0).count(),
                dailyRevenue,
                statusCounts,
                topProducts(valid),
                lowStock);
    }

    private List<TopProduct> topProducts(List<Order> valid) {
        Map<String, TopAccumulator> byProduct = new LinkedHashMap<>();
        valid.forEach(order -> order.lines().forEach(line -> byProduct
                .computeIfAbsent(line.productCode(), c -> new TopAccumulator(line.productName()))
                .add(line)));

        return byProduct.values().stream()
                .map(a -> new TopProduct(a.name, a.quantity, a.revenue))
                .sorted(Comparator.comparingInt(TopProduct::quantity).reversed())
                .limit(5)
                .toList();
    }

    private LocalDate toDate(Instant instant) {
        return instant.atZone(SEOUL).toLocalDate();
    }

    private static final class DailyAccumulator {
        private long revenue;
        private int count;

        void add(long amount) {
            revenue += amount;
            count += 1;
        }
    }

    private static final class TopAccumulator {
        private final String name;
        private int quantity;
        private long revenue;

        TopAccumulator(String name) {
            this.name = name;
        }

        void add(OrderLine line) {
            quantity += line.quantity();
            revenue += line.lineTotal().value();
        }
    }

    public record Stats(long todayRevenue, int todayOrders, long revenueTotal, int orderTotal,
                        long averageOrderValue, long needsAction, int lowStockCount,
                        long soldOutCount, List<DailyRevenue> dailyRevenue,
                        List<StatusCount> statusCounts, List<TopProduct> topProducts,
                        List<InventoryService.LowStockItem> lowStock) {
    }

    public record DailyRevenue(String date, long revenue, int orders) {
    }

    public record StatusCount(String id, String label, long count) {
    }

    public record TopProduct(String name, int quantity, long revenue) {
    }
}
