package com.wltn.foodflow.customer.service;

import com.wltn.foodflow.customer.entity.Customer;
import com.wltn.foodflow.customeritem.entity.CustomerItem;
import com.wltn.foodflow.customeritem.repository.CustomerItemRepository;
import com.wltn.foodflow.item.entity.Item;
import com.wltn.foodflow.item.service.ItemService;
import com.wltn.foodflow.store.Service.StoreService;
import com.wltn.foodflow.store.entity.Store;
import com.wltn.foodflow.store.entity.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnableAspectJAutoProxy(exposeProxy = true)
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    ItemService itemService;

    @Autowired
    StoreService storeService;
    @Autowired
    private CustomerItemRepository customerItemRepository;

    AtomicInteger optimisticFailures = new AtomicInteger(0); // 낙관적 락 실패 수
    AtomicInteger totalSuccess = new AtomicInteger(0);        // 최종 구매 성공 수

    @BeforeEach
    void resetCounters() {
        optimisticFailures.set(0);
        totalSuccess.set(0);
    }

    @Test
    @Transactional
    public void 물건_구매_성공_테스트() {
        Store store = storeService.saveStore("음식점1", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 10);
        Customer customer = customerService.customerSave("맛집헌터", 10000);

        CustomerItem customerItem = customerService.buyItem(customer.getCustomerId(), item.getItemId());
        assertThat(customerItem.getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(customerItem.getItemId()).isEqualTo(item.getItemId());
    }

    @Test
    @Transactional
    public void 물건_구매_실패_테스트() {
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "제육볶음", 8000, 1);
        Customer c1 = customerService.customerSave("c1", 5000);
        Customer c2 = customerService.customerSave("c2", 10000);
        Customer c3 = customerService.customerSave("c3", 10000);

        {
            IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                    () -> customerService.buyItem(c1.getCustomerId(), item.getItemId())
            );
            assertThat(illegalStateException.getMessage()).isEqualTo("포인트보다 비싼 물건을 구매할 수 없습니다.");
        }

        {
            CustomerItem customerItem = customerService.buyItem(c2.getCustomerId(), item.getItemId());
            assertThat(customerItem.getCustomerId()).isEqualTo(c2.getCustomerId());
            assertThat(customerItem.getItemId()).isEqualTo(item.getItemId());
        }

        System.out.println(item.getQuantity());
        {
            IllegalStateException illegalStateException2 = assertThrows(IllegalStateException.class,
                    () -> customerService.buyItem(c3.getCustomerId(), item.getItemId())
            );
            System.out.println(item.getQuantity());
            assertThat(illegalStateException2.getMessage()).isEqualTo("남은 재고가 없습니다.");
        }
    }

    @Test
    @DisplayName("여러_고객이_하나의_물건을_구매")
    public void 여러_고객이_하나의_물건을_구매() throws InterruptedException{

        AtomicInteger lockFailures = new AtomicInteger();
        AtomicInteger stockFailures = new AtomicInteger();

        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 120);

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 응답 시간 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();

        for (int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Customer customer = customerService.customerSave("손님", 10000);
                    customerService.buyItemWithRedisson(customer.getCustomerId(), item.getItemId());

                } catch (CannotAcquireLockException e) {
                    System.out.println("Redisson 락 획득 실패");
                    lockFailures.incrementAndGet();
                } catch (IllegalStateException e) {
                    System.out.println("재고 부족 예외");
                    stockFailures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 응답 시간 측정을 위한 종료 시간 기록
        long endTime = System.currentTimeMillis();  // 또는 Instant.now()

        // 응답 시간 출력
        long duration = endTime - startTime;
        System.out.println("응답 시간: " + duration + "ms");

        executorService.shutdown();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        List<CustomerItem> all = customerItemRepository.findAll();
        System.out.println("최종 구매 성공 수: " + customerItemRepository.findAll().size());
        System.out.println("락 획득 실패 수: " + lockFailures.get());
        System.out.println("재고 부족 예외 수: " + stockFailures.get());
        assertThat(all.size()).isEqualTo(120);
    }

    @Test
    @DisplayName("낙관적 락: 여러 고객이 하나의 물건을 동시에 구매")
    public void 낙관적_락_테스트() throws InterruptedException {
        // given
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 120);
        System.out.println("초기 재고: " + item.getQuantity());

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 응답 시간 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();  // 또는 Instant.now()

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Customer customer = customerService.customerSave("손님", 10000);
                    int retry = 5;
                    while (retry-- > 0) {
                        try {
                            customerService.buyItem(customer.getCustomerId(), item.getItemId());
                            totalSuccess.incrementAndGet();
                            break;
                        } catch (ObjectOptimisticLockingFailureException | IllegalStateException e) {
                            if (e instanceof ObjectOptimisticLockingFailureException) {
                                System.out.println("낙관적 락 실패");
                            } else {
                                System.out.println("재고 부족 예외");
                            }

                            if (retry == 0) optimisticFailures.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 응답 시간 측정을 위한 종료 시간 기록
        long endTime = System.currentTimeMillis();  // 또는 Instant.now()

        // then
        List<CustomerItem> all = customerItemRepository.findAll();
        System.out.println("낙관적 락 테스트 결과");
        System.out.println(" 최종 구매 성공 수: " + totalSuccess.get());
        System.out.println(" 낙관적 락 실패 수: " + optimisticFailures.get());
        System.out.println(" DB에 저장된 구매 수: " + all.size());

        // 응답 시간 출력
        long duration = endTime - startTime;
        System.out.println("응답 시간: " + duration + "ms");

        assertThat(all.size()).isEqualTo(120);
    }


    @Test
    @DisplayName("비관적 락: 여러 고객이 하나의 물건을 동시에 구매")
    public void 비관적_락_테스트() throws InterruptedException {
        // given
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 900);

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger totalSuccess = new AtomicInteger();
        AtomicInteger stockFailures = new AtomicInteger();

        // 응답 시간 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();  // 또는 Instant.now()

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Customer customer = customerService.customerSave("손님", 10000);
                    try {
                        customerService.buyItemWithPessimisticLock(customer.getCustomerId(), item.getItemId());
                        totalSuccess.incrementAndGet();
                    } catch (IllegalStateException e) {
                        System.out.println("재고 부족 예외");
                        stockFailures.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 응답 시간 측정을 위한 종료 시간 기록
        long endTime = System.currentTimeMillis();  // 또는 Instant.now()

        // then
        List<CustomerItem> all = customerItemRepository.findAll();
        System.out.println("비관적 락 테스트 결과");
        System.out.println(" - 최종 구매 성공 수: " + totalSuccess.get());
        System.out.println(" - 재고 부족 예외 수: " + stockFailures.get());
        System.out.println(" - DB에 저장된 구매 수: " + all.size());

        // 응답 시간 출력
        long duration = endTime - startTime;
        System.out.println("응답 시간: " + duration + "ms");

        // 검증: DB에 저장된 구매 수가 요청한 수와 일치해야 함
        assertThat(all.size()).isEqualTo(1000);
    }




    @Test
    public void 하나의_고객이_여러번_물건을_구매한다() throws InterruptedException {
        Store store = storeService.saveStore("store", StoreType.KOREAN);
        Item item = itemService.itemSave(store.getStoreId(), "비빔밥", 8000, 12);
        Customer customer = customerService.customerSave("손님", 45000);

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    customerService.buyItemWithRedisson(customer.getCustomerId(), item.getItemId());

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<CustomerItem> all = customerItemRepository.findAll();
        assertThat(all.size()).isEqualTo(5);
    }
}
