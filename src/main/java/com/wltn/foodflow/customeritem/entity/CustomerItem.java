package com.wltn.foodflow.customeritem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustomerItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long customerItemId;

    private long customerId;
    private long itemId;

}
