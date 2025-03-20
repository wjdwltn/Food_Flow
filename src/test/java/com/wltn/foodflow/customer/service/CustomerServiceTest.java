package com.wltn.foodflow.customer.service;

import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.repository.CustomerItemRepository;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.service.ItemService;
import com.wltn.foodflow.store.Service.StoreService;
import com.wltn.foodflow.store.entity.Store;
import com.wltn.foodflow.store.entity.StoreType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnableAspectJAutoProxy(exposeProxy = true)
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    ItemService itemService;

    @Autowired
    StoreService storeService;
    @Autowired
    private CustomerItemRepository customerItemRepository;

    @Test
    @Transactional
    public void 물건_구매_성공_테스트() {
        Store store = storeService.saveStore("음식점1", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 10);
        Customer customer = customerService.customerSave("맛집헌터", 10000);

        CustomerItem customerItem = customerService.buyItem(customer.getCustomerId(), item.getItemId());
        assertThat(customerItem.getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(customerItem.getItemId()).isEqualTo(item.getItemId());
    }

    @Test
    //@Transactional
    public void 물건_구매_실패_테스트() {
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 1);
        Customer c1 = customerService.customerSave("c1", 5000);
        Customer c2 = customerService.customerSave("c2", 10000);
        Customer c3 = customerService.customerSave("c3", 10000);

        {
            IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                    () -> customerService.buyItem(c1.getCustomerId(), item.getItemId())
            );
            assertThat(illegalStateException.getMessage()).isEqualTo("포인트보다 비싼 물건을 구매할 수 없습니다.");
        }

        {
            CustomerItem customerItem = customerService.buyItem(c2.getCustomerId(), item.getItemId());
            assertThat(customerItem.getCustomerId()).isEqualTo(c2.getCustomerId());
            assertThat(customerItem.getItemId()).isEqualTo(item.getItemId());
        }

        System.out.println(item.getQuantity());
        {
            IllegalStateException illegalStateException2 = assertThrows(IllegalStateException.class,
                    () -> customerService.buyItem(c3.getCustomerId(), item.getItemId())
            );
            System.out.println(item.getQuantity());
            assertThat(illegalStateException2.getMessage()).isEqualTo("남은 재고가 없습니다.");
        }
    }

    @Test
    @DisplayName("여러_고객이_하나의_물건을_구매")
    public void 여러_고객이_하나의_물건을_구매() throws InterruptedException{
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 12);

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Customer customer = customerService.customerSave("손님", 10000);
                    customerService.buyItemWithRedisson(customer.getCustomerId(), item.getItemId());

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<CustomerItem> all = customerItemRepository.findAll();
        assertThat(all.size()).isEqualTo(12);
    }

    @Test
    public void 하나의_고객이_여러번_물건을_구매한다() throws InterruptedException {
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 12);
        Customer customer = customerService.customerSave("손님", 45000);

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    customerService.buyItemWithRedisson(customer.getCustomerId(), item.getItemId());

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<CustomerItem> all = customerItemRepository.findAll();
        assertThat(all.size()).isEqualTo(5);
    }
}
