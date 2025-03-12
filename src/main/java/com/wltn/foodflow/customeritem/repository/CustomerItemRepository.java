package com.wltn.foodflow.customeritem.repository;

import com.wltn.foodflow.customeritem.entity.CustomerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerItemRepository extends JpaRepository<CustomerItem, Long> {
}
