package com.wltn.foodflow.customer.service;

import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customer.repository.CustomerRepository;
import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.service.CustomerItemService;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.repository.ItemRepository;
import com.wltn.foodflow.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final CustomerItemService customerItemService;

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
        Item item = itemService.minusRemained(itemId);
        Customer customer = minusPoint(customerId, item);
        CustomerItem customerItem = customerItemService.customerItemSave(customer.getCustomerId(),item.getItemId());
        return customerItem;
    }


}
