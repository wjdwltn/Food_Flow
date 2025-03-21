<h1>트래픽이 많은 상황에서 동시성 제어하기</h1>

이 서비스는 가게에 존재하는 음식을 판매하는 시스템이다.  
각 음식은 판매 가능한 양이 존재하며, 이를 초과해서는 판매할 수 없다.</n>
또한, 남은 갯수가 0인 음식은 검색 불가하며, 손님은 포인트를 기준으로 음식을 구매할 수 있다. 
음식을 구매하면 그 가격만큼 포인트가 감소하게 된다.


## 요구사항

- **API**가 아닌 **테스트 케이스**로 구현
  -  예외) 캐시 성능 향상을 보기 위해 api로 구현 후 jmeter로 성능 테스트  
- **동시성 문제** 고려
- **트래픽이 많은 상황**을 상정하여 테스트
- **데이터 일관성**을 보장

## 구현

### 1. 재고가 존재하는 모든 요리 확인
- [테스트 로직](https://github.com/wjdwltn/Food_Flow/blob/19deeed4080148d68631119e9fc598c283a44ac0/src/test/java/com/wltn/foodflow/item/service/ItemServiceTest.java#L45)
- [**QueryDSL**](https://github.com/wjdwltn/Food_Flow/blob/19deeed4080148d68631119e9fc598c283a44ac0/src/main/java/com/wltn/foodflow/item/repository/ItemRepositoryImpl.java#L22)을 사용하여 `remain >= 1`인 음식만 조회한다.
- **index**를 생성하여 검색 진행:
    ```sql
    CREATE INDEX idx_storeid_remained ON item (store_id, remained);
    ```
    - 인덱스를 사용하면 속도 향상이 있지만, 속도 저하가 발생하는 경우도 있어 전체 데이터 중 25% 이하가 되도록 설정했다.
    ```sql
    select store_id, remained, item_id from item where 1=1 and store_id = 2 and quantity >= 1;
    ```

  - **성능 비교**  
     인덱스를 쓰지 않은 결과(drop index idx_storeid_remained on item;)에 비해
    - **인덱스 미사용** : 성능 저하 확인.
    
    - **인덱스 사용** : 속도가 크게 향상됨.
    


- **Redis**를 통한 캐싱과 속도 확인
  - [여러 유저가 보는 경우 테스트](https://github.com/wjdwltn/Food_Flow/blob/19deeed4080148d68631119e9fc598c283a44ac0/src/test/java/com/wltn/foodflow/item/service/ItemServiceTest.java#L79)
  - 둘은 동일한 결과를 보여주지만, 속도는 캐시를 통하는게 훨씬 빠르다.
  - **캐시 갱신** : [새로운 음식이 등록될 때마다 캐시를 갱신한다.](https://github.com/wjdwltn/Food_Flow/blob/19deeed4080148d68631119e9fc598c283a44ac0/src/main/java/com/wltn/foodflow/item/service/ItemService.java#L19) [테스트 확인](https://github.com/wjdwltn/Food_Flow/blob/19deeed4080148d68631119e9fc598c283a44ac0/src/test/java/com/wltn/foodflow/item/service/ItemServiceTest.java#L118)
    - 매번 등록할 때에 갱신하는 이유는, 음식의 경우 가게에서 새로운 음식을 등록하는 경우보다 고객이 찾는 경우가 훨씬 많기 때문이다.
    - 일종의 write-through 전략.





## 동시성 문제 해결
###  음식 구매 프로세스

- **음식의 남은 갯수**를 차감합니다.
- **유저의 포인트**를 차감합니다.
- **유저가 구매한 물건** 데이터를 저장합니다.

**테스트 순서**
- **물건 구매 성공 테스트**
- **물건 구매 실패 테스트**
  - SpringBootTest @Transactional로 인한 오류 발생
  -  @Transactional rollback 동작과 격리 수준을 고려한 문제 해결

### Case 1. 여러 고객이 하나의 물건을 구매하는 경우

- **Redisson**을 사용하여 **분산 락**을 적용
  - 지난 프로젝트에서는 간단한 락만 필요했기에 **Lettuce** 사용
  - lettuce보다 빠르고, 여러 DB 테이블에 걸쳐서 진행될 수 있고 분산 환경을 고려하여 **Redisson**을 선택
  - 이를 통해 여러 쓰레드에서 한꺼번에 접근해도 동시성 문제 해결 완료
- 이전 Self-Invocation 해결 경험으로 동일 서비스에서의 내부 호출 오류 해결

### Case 2. 하나의 고객이 여러 번 동일 물건을 구매하는 경우

- 위와 동일하게 **Redisson**을 통해 동시성 문제를 해결

