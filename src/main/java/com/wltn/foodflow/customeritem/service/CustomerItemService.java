package com.wltn.foodflow.customeritem.service;

import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.repository.CustomerItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerItemService {
    private final CustomerItemRepository customerItemRepository;

    public CustomerItem customerItemSave(long customerId, long itemId){
        CustomerItem customerItem = CustomerItem.builder()
                .customerId(customerId)
                .itemId(itemId)
                .build();

        return customerItemRepository.save(customerItem);
    }
}
