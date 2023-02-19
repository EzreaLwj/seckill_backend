package com.ezreal.security.service.impl;

import com.ezreal.security.service.SlidingWindowLimitService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultSlidingWindowLimitServiceImpl implements SlidingWindowLimitService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     *
     * @param userActionKey 用户及行为标识
     * @param period        限流周期，单位毫秒
     * @param size          滑动窗口大小
     * @return
     */
    @Override
    public boolean pass(String userActionKey, int period, int size) {

        long current = System.currentTimeMillis();
        int length = period * size;
        long start = current - length;
        long expireTime = length + period;

        // 添加新的请求
        redisTemplate.opsForZSet().add(userActionKey, String.valueOf(current), current);
        // 过期时间 窗口长度+一个时间间隔
        redisTemplate.expire(userActionKey, expireTime, TimeUnit.MILLISECONDS);
        // 移除[0,start]区间内的值
        redisTemplate.opsForZSet().removeRangeByScore(userActionKey, 0 ,start);

        // 统计个数
        Long count = redisTemplate.opsForZSet().zCard(userActionKey);
        if (count == null) {
            return false;
        }
        return count <= size;
    }
}
