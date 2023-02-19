package com.ezreal.common.lock.redisson;

public interface DistributedLockFactoryService {
    DistributedLock getDistributedLock(String key);
}
