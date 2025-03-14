package com.wltn.foodflow.store.Service;

import com.wltn.foodflow.store.entity.Store;
import com.wltn.foodflow.store.entity.StoreType;
import com.wltn.foodflow.store.repository.StoreRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public Store saveStore(String storeName, StoreType storeType) {
        Store store = Store.builder()
                .storeName(storeName)
                .storeType(storeType)
                .build();

        return storeRepository.save(store);
    }

}
