package com.wltn.foodflow.item.service;

import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customer.service.CustomerService;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.store.Service.StoreService;
import com.wltn.foodflow.store.entity.Store;
import com.wltn.foodflow.store.entity.StoreType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ItemServiceTest {

    @Autowired
    ItemService itemService;

    @Autowired
    StoreService storeService;

    @Autowired
    CustomerService customerService;

    @Test
    public void 아이템_저장() {

        // given
        Store store = storeService.saveStore("테스트", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "제육볶음", 12000, 10);

        // then
        assertThat(item.getItemName()).isEqualTo("제육볶음");
    }

    @Test
    @DisplayName("아이템_존재_여부_테스트")
    public void 아이템_존재_여부_테스트(){
        Store store = storeService.saveStore("음식점1", StoreType.KOREAN);
        Store store2 = storeService.saveStore("음식점2", StoreType.KOREAN);
        Item item1 = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 10);
        Item item2 = itemService.itemSave(store.getStoreId(), "육회", 18000, 10);
        Item item3 = itemService.itemSave(store.getStoreId(), "잡채", 10000, 0);
        Item item4 = itemService.itemSave(store.getStoreId(), "불고기", 10000, 0);

        ArrayList<Item> itemList = new ArrayList<>();
        itemList.add(item1);
        itemList.add(item2);
        for(int i=0; i<30; i++) {
            itemList.add(itemService.itemSave(store.getStoreId(), "테스트데이터", 10000, 10));
        }
        for(int i=0; i<3000; i++) {
            itemService.itemSave(store2.getStoreId(), "테스트데이터", 10000, 10);
        }
        ArrayList<Item> itemNotRemainingList = new ArrayList<>();
        itemNotRemainingList.add(item3);
        itemNotRemainingList.add(item4);

        List<Item> itemRemainList = itemService.showRemainItemListByStoreId(store.getStoreId());

        itemList.forEach(
                item -> assertThat(itemRemainList.contains(item)).isTrue()
        );

        itemNotRemainingList.forEach(
                item -> assertThat(!itemRemainList.contains(item)).isTrue()
        );
    }

    @Test
    @DisplayName(" 여러_유저가_보는_경우")
    public void 여러_유저가_보는_경우() {
        Store store = this.storeService.saveStore("음식점1", StoreType.KOREAN);
        this.itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 10);

        // 동일성 테스트
        List<Item> itemsWithoutCache = itemService.showRemainItemListByStoreId(store.getStoreId());
        List<Item> itemsWithCache = itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());

        for (int i=0; i<itemsWithoutCache.size(); i++) {
            assertThat(itemsWithoutCache.get(i).getItemId()).isEqualTo(itemsWithCache.get(i).getItemId());
        }

        long startTime = System.currentTimeMillis();

        for (int i=0; i<50000; i++) {
            itemService.showRemainItemListByStoreId(store.getStoreId());
        }
        long stopTime = System.currentTimeMillis();

        long timeGapForDatabase = (stopTime - startTime);

        // 캐시를 통해 가져오기
        startTime = System.currentTimeMillis();

        for (int i=0; i<50000; i++) {
            itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());
        }
        stopTime = System.currentTimeMillis();

        long timeGapForCache = (stopTime - startTime);

        // 확인
        assertThat(timeGapForDatabase).isGreaterThan(timeGapForCache);
        System.out.println("데이터베이스 : " + timeGapForDatabase);
        System.out.println("캐시 : " + timeGapForCache);
    }

    @Test
    @DisplayName("데이터_등록시_갱신_테스트")
    public void 데이터_등록시_갱신_테스트() {
        Store store = this.storeService.saveStore("음식점1", StoreType.KOREAN);
        this.itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 10);

        List<Item> itemsWithCacheFirst = itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());

        itemService.itemSave(store.getStoreId(), "비빔국수", 8500, 10);
        List<Item> itemsWithCacheSecond = itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());

        assertThat(itemsWithCacheFirst).isNotEqualTo(itemsWithCacheSecond);
        assertThat(itemsWithCacheFirst.size()).isLessThan(itemsWithCacheSecond.size());
    }

    @Test
    @DisplayName("재고_없을때_안보이는_테스트")
    public void 재고_없을때_안보이는_테스트() {
        Store store = storeService.saveStore("음식점1", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 1);

        List<Item> itemsWithCacheFirst = itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());

        Customer customer = customerService.customerSave("밥매니아", 30000);
        customerService.buyItem(customer.getCustomerId(), item.getItemId());

        List<Item> itemsWithCacheSecond = itemService.showRemainItemListByStoreIdWithCache(store.getStoreId());

        assertThat(itemsWithCacheFirst).isNotEqualTo(itemsWithCacheSecond);
        assertThat(itemsWithCacheFirst.size()).isGreaterThan(itemsWithCacheSecond.size());
    }
}