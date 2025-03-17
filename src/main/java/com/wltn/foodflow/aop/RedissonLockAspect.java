package com.wltn.foodflow.aop;

import com.wltn.foodflow.config.CustomSpringELParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedissonLockAspect {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;

    @Around("@annotation(com.wltn.foodflow.aop.RedissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonLock annotation = method.getAnnotation(RedissonLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), annotation.key());
        RLock rLock = redissonClient.getLock(key);

        try {
            boolean available = rLock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.SECONDS); // Lock 획득 시도

            if (!available){
                log.info("구매 과정 중 lock 획득 실패={}", key);
                return false;
            }
            log.info("Lock 획득 성공: {}", key);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            log.info("에러 발생");
            throw e;
        } finally {
            try {
                rLock.unlock(); // Lock 해제
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock {} {}", method.getName(), key);
            }
        }
    }
}
