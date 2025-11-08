package io.github.aikobn26.teamprogressviz.shared.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class KeyLockManager {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void runWithLock(String key, Runnable action) {
        callWithLock(key, () -> {
            action.run();
            return null;
        });
    }

    public <T> T callWithLock(String key, Supplier<T> supplier) {
        if (key == null || supplier == null) {
            throw new IllegalArgumentException("key and supplier must not be null");
        }

        ReentrantLock lock = locks.computeIfAbsent(key, unused -> new ReentrantLock());
        lock.lock();
        boolean releaseAfterCompletion = registerUnlockAfterTransaction(key, lock);
        try {
            return supplier.get();
        } finally {
            if (!releaseAfterCompletion) {
                unlockAndCleanup(key, lock);
            }
        }
    }

    private boolean registerUnlockAfterTransaction(String key, ReentrantLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                unlockAndCleanup(key, lock);
            }
        });
        return true;
    }

    private void unlockAndCleanup(String key, ReentrantLock lock) {
        try {
            lock.unlock();
        } finally {
            cleanupLock(key, lock);
        }
    }

    private void cleanupLock(String key, ReentrantLock lock) {
        if (!lock.isLocked() && !lock.hasQueuedThreads()) {
            locks.remove(key, lock);
        }
    }
}
