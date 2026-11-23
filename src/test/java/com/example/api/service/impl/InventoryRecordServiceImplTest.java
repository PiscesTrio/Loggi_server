package com.example.api.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.InventoryType;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WarehouseRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Stock in/out: what moves, and what refuses to move.
 *
 * <p>The first four of these began as characterization tests, with a note that the source's {@code
 * if (optional == null)} was a dead branch — findById never returns null, so a missing commodity
 * fell through to {@code optional.get()} and NoSuchElementException — for the rewrite to fix and
 * assert. The rewrite has happened; the note is now the test below it.
 *
 * <p>The refusals carry a status apiece rather than the single flattened 400 they used to: a
 * missing id is 404, insufficient stock is 409, a nonsense quantity is 400.
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

    private static InventoryRecord movement(int count) {
        InventoryRecord rec = new InventoryRecord();
        rec.setWarehouse(warehouse("w1"));
        rec.setCommodity(commodity("c1"));
        rec.setCount(count);
        return rec;
    }

    @Test
    @DisplayName("out throws when stock is insufficient")
    void out_whenStockInsufficient_throws() {
        // The conditional UPDATE changed nothing, but the row exists: so there was not enough.
        when(inventoryRepository.withdraw(eq("w1"), eq("c1"), eq(10), any())).thenReturn(0);
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1"))
                .thenReturn(new Inventory());

        // 409: the request is well formed, and would be valid against a different stock
        // level. Flattened into 400 the client could not tell it apart from a malformed
        // body, which is the difference between "ask for fewer" and "fix your code".
        assertThatThrownBy(() -> service.out(movement(10)))
                .isInstanceOf(BizException.class)
                .hasMessage("not enough stock to send out")
                .extracting(e -> ((BizException) e).getStatus())
                .isEqualTo(409);
        verifyNoInteractions(commodityRepository, recordRepository);
    }

    @Test
    @DisplayName("out throws when the warehouse has no such item")
    void out_whenItemNotInWarehouse_throws() {
        when(inventoryRepository.withdraw(eq("w1"), eq("c1"), eq(1), any())).thenReturn(0);
        when(inventoryRepository.findByWarehouseIdAndCommodityId("w1", "c1")).thenReturn(null);

        assertThatThrownBy(() -> service.out(movement(1)))
                .isInstanceOf(BizException.class)
                .hasMessage("the warehouse does not hold that commodity")
                .extracting(e -> ((BizException) e).getStatus())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("out refuses when the commodity total disagrees with the warehouse")
    void out_whenCommodityTotalWouldGoNegative_throws() {
        // The warehouse row had the stock but the total did not. Throwing is what rolls the
        // withdrawal back; carrying on would make the disagreement permanent.
        when(inventoryRepository.withdraw(eq("w1"), eq("c1"), eq(4), any())).thenReturn(1);
        when(commodityRepository.adjustCount(eq("c1"), eq(-4), any())).thenReturn(0);

        assertThatThrownBy(() -> service.out(movement(4)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        verifyNoInteractions(recordRepository);
    }

    @Test
    @DisplayName("out sets type OUT and decrements both counters in the database, not in memory")
    void out_happyPath_setsTypeOutAndDecrementsBothCounters() {
        // The decrement is the database's arithmetic now, so what this pins is that both
        // conditional UPDATEs are issued with the right sign. Whether they hold under
        // concurrency is ConcurrentStockMovementIT's question; a mock cannot answer it.
        Commodity c = commodity("c1");
        c.setName("牛奶");
        when(inventoryRepository.withdraw(eq("w1"), eq("c1"), eq(4), any())).thenReturn(1);
        when(commodityRepository.adjustCount(eq("c1"), eq(-4), any())).thenReturn(1);
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(c));
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.out(movement(4));

        assertThat(saved.getType()).isEqualTo(InventoryType.OUT); // pin it: out => OUT
        assertThat(saved.getName()).isEqualTo("牛奶");
        // createAt: see AuditingIT — filled by the auditing listener, not by this service.
        verify(inventoryRepository, never()).save(any());
        verify(commodityRepository, never()).save(any());
    }

    @Test
    @DisplayName("in sets type IN and creates inventory for a new item")
    void in_newItem_setsTypeInAndCreatesInventory() {
        InventoryRecord rec = movement(5);
        rec.setName("牛奶");
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(commodity("c1")));
        // No row to add to: the warehouse has never held this commodity.
        when(inventoryRepository.deposit(eq("w1"), eq("c1"), eq(5), any())).thenReturn(0);
        // Creating the inventory row now resolves the warehouse instead of copying an id
        // string into it, so a warehouse that does not exist can no longer be referenced.
        when(warehouseRepository.findById("w1")).thenReturn(Optional.of(warehouse("w1")));
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.in(rec);

        assertThat(saved.getType()).isEqualTo(InventoryType.IN); // pin it: in => IN
        // createAt: see AuditingIT — filled by the auditing listener, not by this service.
        verify(inventoryRepository).saveAndFlush(argThat(i -> i.getCount() == 5));
        verify(commodityRepository).adjustCount(eq("c1"), eq(5), any());
    }

    @Test
    @DisplayName("in adds to an existing row without creating another")
    void in_existingItem_depositsWithoutInsert() {
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(commodity("c1")));
        when(inventoryRepository.deposit(eq("w1"), eq("c1"), eq(5), any())).thenReturn(1);
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        service.in(movement(5));

        verify(inventoryRepository, never()).saveAndFlush(any());
        verifyNoInteractions(warehouseRepository);
        verify(commodityRepository).adjustCount(eq("c1"), eq(5), any());
    }

    @Test
    @DisplayName("A commodity id that does not exist is refused, not dereferenced")
    void in_whenCommodityMissing_throws404() {
        // The branch the characterization tests flagged. `if (optional == null)` could never
        // be true, so the guard's message never reached anyone and the next line threw
        // NoSuchElementException from inside the Optional instead — a guard that reads
        // like validation while the real failure walks straight past it.
        InventoryRecord rec = movement(1);
        rec.setCommodity(commodity("ghost"));
        when(commodityRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.in(rec))
                .isInstanceOf(BizException.class)
                // The code, not the sentence. The status says 404 and the sentence says which
                // 404 — but only one of the two is something a client can branch on, and it
                // is the one worth pinning.
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMODITY_NOT_FOUND);
        verifyNoInteractions(inventoryRepository, recordRepository);
    }

    @Test
    @DisplayName("A negative inbound quantity is refused before anything is written")
    void in_whenCountNegative_throwsAndWritesNothing() {
        // Unguarded, this drains stock through the method called "in" and leaves a
        // movement record claiming the opposite of what happened.
        InventoryRecord rec = movement(-5);

        assertThatThrownBy(() -> service.in(rec))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getStatus())
                .isEqualTo(400);
        verifyNoInteractions(commodityRepository, inventoryRepository, recordRepository);
    }
}
