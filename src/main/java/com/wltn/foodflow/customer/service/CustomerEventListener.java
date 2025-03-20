package com.wltn.foodflow.customer.service;


import com.wltn.foodflow.item.entity.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
public class CustomerEventListener {
    @Autowired
    private CustomerService customerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void transactionalEventListenerAfterCommit(Item itemEvent) {
        customerService.refreshItemCache(itemEvent.getStoreId());
    }
}
