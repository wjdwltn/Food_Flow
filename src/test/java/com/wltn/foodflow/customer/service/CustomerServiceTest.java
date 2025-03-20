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
}
