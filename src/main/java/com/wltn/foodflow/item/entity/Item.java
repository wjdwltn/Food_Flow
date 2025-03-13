package com.wltn.foodflow.item.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long itemId;
    private String itemName;
    private int price;
    private int quantity;
    private long storeId;

    public int minusQuantity() {
        if (quantity == 0) {
            throw new IllegalStateException("남은 재고가 없습니다.");
        }

        return quantity - 1;
    }
}
