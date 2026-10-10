package com.example.payment;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

public final class TestTx {
    private TestTx() {}
    public static TransactionTemplate template() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            public TransactionStatus getTransaction(TransactionDefinition d) { return new SimpleTransactionStatus(); }
            public void commit(TransactionStatus s) {}
            public void rollback(TransactionStatus s) {}
        });
    }
}