package com.wltn.foodflow.item.controller;

import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // 캐시를 사용하지 않는 API
    @GetMapping("/store/{storeId}/items")
    public ResponseEntity<List<Item>> getItemsByStoreId(@PathVariable long storeId) {
        List<Item> items = itemService.showRemainItemListByStoreId(storeId);
        if (items.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(items);
    }

    // 캐시를 사용하는 API
    @GetMapping("/store/{storeId}/items-with-cache")
    public ResponseEntity<List<Item>> getItemsByStoreIdWithCache(@PathVariable long storeId) {
        List<Item> items = itemService.showRemainItemListByStoreIdWithCache(storeId);
        if (items.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(items);
    }
}