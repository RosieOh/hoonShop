package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductCodeValue(String code);

    List<Inventory> findByProductCodeValueIn(List<String> codes);

    /**
     * {@code SELECT ... FOR UPDATE}.
     *
     * <p>같은 상품을 동시에 결제하면 뒤에 온 트랜잭션이 앞 트랜잭션의 커밋을 기다립니다.
     * 이게 없으면 둘 다 "재고 1개 남음"을 읽고 각자 1개씩 빼서 -1이 됩니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productCode.value = :code")
    Optional<Inventory> findForUpdate(@Param("code") String code);

    @Query("select i from Inventory i where i.quantity <= :threshold order by i.quantity asc")
    List<Inventory> findLowStock(@Param("threshold") int threshold);
}
