package com.wltn.foodflow.item.repository;

import com.wltn.foodflow.item.entity.Item;

import java.util.List;


public interface ItemRepositoryCustom {
    List<Item> selectItemListByStoreId(long storeId);

    List<Item> showRemainItemListByStoreIdOrderBySell(long storeId);
}
