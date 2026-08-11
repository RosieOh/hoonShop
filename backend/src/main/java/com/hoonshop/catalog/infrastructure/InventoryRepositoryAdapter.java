package com.hoonshop.catalog.infrastructure;

import com.hoonshop.catalog.domain.Inventory;
import com.hoonshop.catalog.domain.InventoryRepository;
import com.hoonshop.catalog.domain.ProductCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryJpaRepository jpa;

    public InventoryRepositoryAdapter(InventoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Inventory> findByProductCode(ProductCode code) {
        return jpa.findByProductCodeValue(code.value());
    }

    @Override
    public Optional<Inventory> findForUpdate(ProductCode code) {
        return jpa.findForUpdate(code.value());
    }

    @Override
    public List<Inventory> findAllByProductCodes(List<ProductCode> codes) {
        return jpa.findByProductCodeValueIn(codes.stream().map(ProductCode::value).toList());
    }

    @Override
    public List<Inventory> findLowStock(int threshold) {
        return jpa.findLowStock(threshold);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return jpa.save(inventory);
    }
}
