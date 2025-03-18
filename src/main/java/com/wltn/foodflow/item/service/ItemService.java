package com.wltn.foodflow.item.service;

import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional
    @CacheEvict(value = "itemCache", key = "#storeId")
    public Item itemSave(long storeId, String itemName, int price, int quantity) {
        Item item = Item.builder()
                .storeId(storeId)
                .itemName(itemName)
                .price(price)
                .quantity(quantity)
                .build();

        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<Item> showRemainItemListByStoreId(long storeId) {
        return this.itemRepository.selectItemListByStoreId(storeId);
    }

    @Cacheable(value = "itemCache", key = "#storeId")
    @Transactional(readOnly = true)
    public List<Item> showRemainItemListByStoreIdWithCache(long storeId) {
        return this.itemRepository.selectItemListByStoreId(storeId);
    }


    @CacheEvict(value = "itemCache", key = "#result.storeId", condition = "#result.quantity == 0")
    public Item minusQuantity(long itemId) {
        Item item = this.itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("해당하는 물품이 없습니다."));

        if (item.getQuantity() == 0) {
            throw new IllegalStateException("남은 재고가 없습니다.");
        }

        item.setQuantity(item.getQuantity() - 1);
        return item;
    }

    @Transactional(readOnly = true)
    public List<Item> showRemainItemListByStoreIdOrderBySell(long storeId) {
        return this.itemRepository.showRemainItemListByStoreIdOrderBySell(storeId);
    }
}