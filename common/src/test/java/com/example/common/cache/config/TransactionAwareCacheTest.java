package com.example.common.cache.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TransactionAwareCacheTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotEvictTheBackingCacheWhenTheTransactionRollsBack() {
        Cache backingCache = mock(Cache.class);
        Cache transactionAwareCache = new TransactionAwareCacheDecorator(backingCache);
        TransactionSynchronizationManager.initSynchronization();

        transactionAwareCache.evict("report-1");
        TransactionSynchronizationManager.clearSynchronization();

        verify(backingCache, never()).evict("report-1");
    }
}
