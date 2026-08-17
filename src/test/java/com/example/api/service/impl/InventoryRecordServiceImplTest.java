package com.example.api.service.impl;

import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
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
 * Characterization tests for the stock in/out logic.
 * Note: the source's `if (optional == null)` (findById never returns null) is a
 * dead branch; a truly missing commodity blows up at optional.get() with
 * NoSuchElementException — left for the InventoryRecord rewrite slice to fix and assert.
 */
@ExtendWith(MockitoExtension.class)
class InventoryRecordServiceImplTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock CommodityRepository commodityRepository;
    @Mock InventoryRecordRepository recordRepository;
    @InjectMocks InventoryRecordServiceImpl service;

    @Test
    @DisplayName("out throws when stock is insufficient")
    void out_whenStockInsufficient_throws() {
        InventoryRecord rec = new InventoryRecord();
        rec.setWid("w1"); rec.setCid("c1"); rec.setCount(10);
        Inventory inv = new Inventory(); inv.setCount(3);
        when(inventoryRepository.findByWidAndCid("w1", "c1")).thenReturn(inv);

        assertThatThrownBy(() -> service.out(rec))
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("出库失败，库存数量不足");
    }

    @Test
    @DisplayName("out throws when the warehouse has no such item")
    void out_whenItemNotInWarehouse_throws() {
        InventoryRecord rec = new InventoryRecord();
        rec.setWid("w1"); rec.setCid("c1"); rec.setCount(1);
        when(inventoryRepository.findByWidAndCid("w1", "c1")).thenReturn(null);

        assertThatThrownBy(() -> service.out(rec))
                .hasMessage("仓库内不存在该商品");
    }

    @Test
    @DisplayName("out sets type=-1 and decrements both the commodity total and the warehouse inventory")
    void out_happyPath_setsTypeMinusOneAndDecrementsBothCounters() throws Exception {
        // The counterpart of in_newItem_...: without this, the whole out-direction
        // mutation block (InventoryRecordServiceImpl:67-79) has zero coverage, and the
        // InventoryRecord rewrite slice could flip the sign or drop a decrement
        // without any test going red.
        InventoryRecord rec = new InventoryRecord();
        rec.setWid("w1"); rec.setCid("c1"); rec.setCount(4);
        Inventory inv = new Inventory(); inv.setCount(10);
        Commodity c = new Commodity(); c.setCount(10);
        when(inventoryRepository.findByWidAndCid("w1", "c1")).thenReturn(inv);
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(c));
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.out(rec);

        assertThat(saved.getType()).isEqualTo(-1);      // pin it: out => type = -1
        assertThat(saved.getCreateAt()).isNotNull();
        assertThat(c.getCount()).isEqualTo(6);          // commodity total: 10 - 4
        assertThat(inv.getCount()).isEqualTo(6);        // warehouse inventory: 10 - 4
        verify(commodityRepository).save(c);
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("in sets type=+1 and creates inventory for a new item")
    void in_newItem_setsTypePositiveOneAndCreatesInventory() throws Exception {
        InventoryRecord rec = new InventoryRecord();
        rec.setWid("w1"); rec.setCid("c1"); rec.setCount(5); rec.setName("牛奶");
        Commodity c = new Commodity(); c.setCount(0);
        when(commodityRepository.findById("c1")).thenReturn(Optional.of(c));
        when(inventoryRepository.findByWidAndCid("w1", "c1")).thenReturn(null);
        when(recordRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InventoryRecord saved = service.in(rec);

        assertThat(saved.getType()).isEqualTo(1);     // pin it: in => type = +1
        assertThat(saved.getCreateAt()).isNotNull();
        verify(inventoryRepository).save(any(Inventory.class));
    }
}
