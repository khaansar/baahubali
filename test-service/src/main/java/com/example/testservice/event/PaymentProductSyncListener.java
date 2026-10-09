package com.example.testservice.event;

import com.example.testservice.client.PaymentProductSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProductSyncListener {
    private final PaymentProductSyncClient paymentProductSyncClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProductSync(PaymentProductSyncEvent event) {
        log.debug("Synchronising payment product for test series {}", event.seriesId());
        paymentProductSyncClient.syncSeries(event.seriesId(), event.title(), event.active(), event.basePriceRupees());
    }
}