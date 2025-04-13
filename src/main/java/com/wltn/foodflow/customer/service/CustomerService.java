package com.wltn.foodflow.customer.service;

import com.wltn.foodflow.aop.RedissonLock;
import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customer.repository.CustomerRepository;
import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.service.CustomerItemService;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.repository.ItemRepository;
import com.wltn.foodflow.item.service.ItemService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService{
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final ItemRepository itemRepository;
    private final CustomerItemService customerItemService;
    private final RedissonClient redissonClient;

    @Autowired
    private CustomerService self;

    private final String BUY_ITEM_KEY = "BUY_ITEM_REDISSON_KEY";

    @Transactional
    public Customer customerSave(String customerName, int point) {
        Customer customer = Customer.builder()
                .customerName(customerName)
                .point(point)
                .build();

        return customerRepository.save(customer);
    }

    public Customer minusPoint(long customerId, Item item){
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("해당하는 유저가 없습니다."));
        int pointAfterBuy = customer.getPoint() - item.getPrice();

        if(pointAfterBuy < 0){
            throw new IllegalStateException("포인트보다 비싼 물건을 구매할 수 없습니다.");
        }

        customer.setPoint(pointAfterBuy);
        return customer;
    }

    @Transactional
    public CustomerItem buyItem(long customerId, long itemId){
        Item item = itemService.minusQuantity(itemId);
        Customer customer = minusPoint(customerId, item);
        return customerItemService.customerItemSave(customer.getCustomerId(),item.getItemId());
    }

    @Transactional
    public CustomerItem buyItemWithPessimisticLock(long customerId, long itemId){
        Item item = itemService.minusQuantityWithPessimisticLock(itemId);
        Customer customer = minusPoint(customerId, item);
        return customerItemService.customerItemSave(customer.getCustomerId(),item.getItemId());
    }

    public void buyItemWithRedisson(long customerId, long itemId) {
        RLock rLock = redissonClient.getLock(BUY_ITEM_KEY);

        try {
            boolean available = rLock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!available) {
                throw new RuntimeException("구매 과정 중 lock 획득 실패");
            }

           self.buyItem(customerId, itemId);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            rLock.unlock();
            System.out.println("!!!!Lock 해제");
        }
    }

    @CacheEvict(value = "itemCache", key = "#storeId")
    public void refreshItemCache(long storeId) {
        System.out.println("Cache evicted for storeId: " + storeId);
    }
}
