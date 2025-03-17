package com.wltn.foodflow.customer.service;

import com.wltn.foodflow.aop.RedissonLock;
import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customer.repository.CustomerRepository;
import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.service.CustomerItemService;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.repository.ItemRepository;
import com.wltn.foodflow.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final CustomerItemService customerItemService;
    private final RedissonClient redissonClient;


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

    public CustomerItem buyItem(long customerId, long itemId){
        Item item = itemService.minusQuantity(itemId);
        Customer customer = minusPoint(customerId, item);
        return customerItemService.customerItemSave(customer.getCustomerId(),item.getItemId());
    }


    @Transactional
    @RedissonLock(key = "#customerId")
    public CustomerItem buyItemWithRedisson(long customerId, long itemId) {
        return buyItem(customerId, itemId);
    }
}
