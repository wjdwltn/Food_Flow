package com.wltn.foodflow.item.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wltn.foodflow.item.entity.Item;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.wltn.foodflow.customeritem.entity.QCustomerItem.customerItem;
import static com.wltn.foodflow.item.entity.QItem.item;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ItemRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Item> selectItemListByStoreId(long storeId) {
        return queryFactory
            .selectFrom(item)
            .where(
                    item.storeId.eq(storeId),
                    item.quantity.goe(1)
            )
            .fetch();
    }

    @Override
    public List<Item> showRemainItemListByStoreIdOrderBySell(long storeId) {
        return queryFactory
            .select(item)
            .from(item)
            .where(
                    item.storeId.eq(storeId),
                    item.quantity.goe(1)
            )
            .join(customerItem).on(item.itemId.eq(customerItem.itemId))
            .groupBy(customerItem.itemId)
            .orderBy(customerItem.itemId.desc())
            .fetch();
    }
}
