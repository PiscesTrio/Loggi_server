package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.InventoryType;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Stock in/out: what moves, and what refuses to move.
 *
 * <p>S00 wrote the first four of these as characterization tests and left a note that the
 * source's {@code if (optional == null)} was a dead branch — findById never returns null,
 * so a missing commodity fell through to {@code optional.get()} and NoSuchElementException
 * — for the rewrite slice to fix and assert. This is that slice; the note is now the test
 * below it.
 *
 * <p>The refusals carry a status apiece rather than the single flattened 400 they used to:
 * a missing id is 404, insufficient stock is 409, a nonsense quantity is 400.
 */
@ExtendWith(MockitoExtension.class)
class InventoryRecordServiceImplTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock CommodityRepository commodityRepository;
    @Mock InventoryRecordRepository recordRepository;
    @Mock WarehouseRepository warehouseRepository;
    @InjectMocks InventoryRecordServiceImpl service;

    /** A reference, as a request carries one: an entity holding nothing but an id. */
    private static Warehouse warehouse(String id) {
        Warehouse w = new Warehouse();
        w.setId(id);
        return w;
    }

    private static Commodity commodity(String id) {
        Commodity c = new Commodity();
        c.setId(id);
        return c;
    }

    @Test
    @DisplayName("out throws when stock is insufficient")
    void out_whenStockInsufficient_throws() {
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("c1")); rec.setCount(10);
        Inventory inv = new Inventory(); inv.setCount(3);
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1")).thenReturn(inv);

        // 409: the request is well formed, and would be valid against a different stock
        // level. Flattened into 400 the client could not tell it apart from a malformed
        // body, which is the difference between "ask for fewer" and "fix your code".
        assertThatThrownBy(() -> service.out(rec))
                .isInstanceOf(BizException.class)
                .hasMessage("出库失败，库存数量不足")
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("out throws when the warehouse has no such item")
    void out_whenItemNotInWarehouse_throws() {
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("c1")); rec.setCount(1);
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1")).thenReturn(null);

        assertThatThrownBy(() -> service.out(rec))
                .isInstanceOf(BizException.class)
                .hasMessage("仓库内不存在该商品")
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("out sets type=-1 and decrements both the commodity total and the warehouse inventory")
    void out_happyPath_setsTypeOutAndDecrementsBothCounters() throws Exception {
        // The counterpart of in_newItem_...: without this, the whole out-direction
        // mutation block (InventoryRecordServiceImpl:67-79) has zero coverage, and the
        // InventoryRecord rewrite slice could flip the sign or drop a decrement
        // without any test going red.
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("c1")); rec.setCount(4);
        Inventory inv = new Inventory(); inv.setCount(10);
        Commodity c = new Commodity(); c.setCount(10);
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1")).thenReturn(inv);
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(c));
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.out(rec);

        assertThat(saved.getType()).isEqualTo(InventoryType.OUT);   // pin it: out => OUT
        // createAt: see AuditingIT — filled by the auditing listener, not by this service.
        assertThat(c.getCount()).isEqualTo(6);          // commodity total: 10 - 4
        assertThat(inv.getCount()).isEqualTo(6);        // warehouse inventory: 10 - 4
        verify(commodityRepository).save(c);
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("in sets type=+1 and creates inventory for a new item")
    void in_newItem_setsTypeInAndCreatesInventory() throws Exception {
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("c1")); rec.setCount(5); rec.setName("牛奶");
        Commodity c = new Commodity(); c.setCount(0);
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(c));
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1")).thenReturn(null);
        // Creating the inventory row now resolves the warehouse instead of copying an id
        // string into it, so a warehouse that does not exist can no longer be referenced.
        when(warehouseRepository.findById("w1")).thenReturn(Optional.of(warehouse("w1")));
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.in(rec);

        assertThat(saved.getType()).isEqualTo(InventoryType.IN);    // pin it: in => IN
        // createAt: see AuditingIT — filled by the auditing listener, not by this service.
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("A commodity id that does not exist is refused, not dereferenced")
    void in_whenCommodityMissing_throws404() {
        // The branch S00 flagged. `if (optional == null)` could never be true, so the
        // guard's message never reached anyone and the next line threw
        // NoSuchElementException from inside the Optional instead — a guard that reads
        // like validation while the real failure walks straight past it.
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("ghost")); rec.setCount(1);
        when(commodityRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.in(rec))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在的商品id")
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(404);
        verifyNoInteractions(inventoryRepository, recordRepository);
    }

    @Test
    @DisplayName("A negative inbound quantity is refused before anything is written")
    void in_whenCountNegative_throwsAndWritesNothing() {
        // Unguarded, this drains stock through the method called "in" and leaves a
        // movement record claiming the opposite of what happened.
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1")); rec.setCommodity(commodity("c1")); rec.setCount(-5);

        assertThatThrownBy(() -> service.in(rec))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(400);
        verifyNoInteractions(commodityRepository, inventoryRepository, recordRepository);
    }
}
