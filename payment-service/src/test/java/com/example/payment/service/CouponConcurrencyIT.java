package com.example.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.payment.entity.Coupon;
import com.example.payment.enums.DiscountType;
import com.example.payment.repository.CouponRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CouponConcurrencyIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    CouponRepository coupons;

    @Autowired
    TransactionTemplate tx;

    @Test
    void neverExceedsUsageLimit() throws Exception {
        Coupon c = new Coupon();

        c.setCode("RACE");
        c.setDiscountType(DiscountType.FIXED_AMOUNT);
        c.setDiscountValue(100);
        c.setStartsAt(Instant.now().minusSeconds(60));
        c.setExpiresAt(Instant.now().plusSeconds(3600));
        c.setUsageLimit(10);

        UUID id = coupons.save(c).getId();

        var pool = Executors.newFixedThreadPool(50);
        var ok = new AtomicInteger();
        var latch = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            fs.add(pool.submit(() -> {
                latch.await();

                if (tx.execute(s -> coupons.tryIncrementUsage(id)) == 1) {
                    ok.incrementAndGet();
                }

                return null;
            }));
        }

        latch.countDown();

        for (var f : fs) {
            f.get();
        }

        assertEquals(10, ok.get());

        assertEquals(10, coupons.findById(id).orElseThrow().getUsedCount());
    }
}