package com.ezreal.order.scheduler;

import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.query.SeckillGoodQuery;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.MultiSeckillGoodsResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.order.service.GoodStockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Time;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ezreal.common.constant.LockKeyConstant.SCHEDULED_WARM_UP_LOCK;

@Component
@Slf4j
public class SeckillGoodWarmUpScheduler {

    private Logger logger = LoggerFactory.getLogger(SeckillGoodWarmUpScheduler.class);

    @Resource
    private SeckillGoodClient seckillGoodClient;

    @Autowired
    private GoodStockCacheService goodStockCacheService;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    @Scheduled(cron = "0 5 16 * * ? ")
    public void warmUpFlashItemTask() {
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(SCHEDULED_WARM_UP_LOCK.getKeyName());
        try {
            // 获取分布式锁
            boolean tryLock = distributedLock.tryLock(0, -1, TimeUnit.SECONDS);
            if (tryLock) {
                logger.info("warmUpFlashItemTask|开启定时任务");
                // 获取所有未预热的商品列表
                SeckillGoodQuery seckillGoodQuery = new SeckillGoodQuery()
                        .setStockWarmUp(0);
                BaseResponse<MultiSeckillGoodsResponse> allSeckillGoods = seckillGoodClient.getAllSeckillGoods(-1L, seckillGoodQuery);
                if (allSeckillGoods == null) {
                    logger.info("获取商品列表返回值失败");
                    return;
                }

                if (allSeckillGoods.getData() == null || allSeckillGoods.getData().getTotal() == null || allSeckillGoods.getData().getTotal() == 0) {
                    logger.info("当前预热商品为空");
                    return;
                }
                // 将未预热商品的库存存入redis，并修改预热状态
                List<SeckillGoodResponse> seckillGoodResponses = allSeckillGoods.getData().getSeckillGoodResponses();
                seckillGoodResponses.forEach(seckillGoodResponse -> {
                    boolean isSuccess = goodStockCacheService.alignItemStocks(seckillGoodResponse.getActivityId(), seckillGoodResponse.getId());

                    if (!isSuccess) {
                        logger.info("秒杀品库存校准失败|{}, {}", seckillGoodResponse.getActivityId(), seckillGoodResponse.getId());
                        return;
                    }
                    UpdateSeckillGoodRequest updateSeckillGoodRequest = new UpdateSeckillGoodRequest()
                            .setStockWarmUp(1);
                    seckillGoodClient.updateSeckillGood(-1L, seckillGoodResponse.getId(), updateSeckillGoodRequest);
                    logger.info("秒杀品库存校准成功|{}, {}", seckillGoodResponse.getActivityId(), seckillGoodResponse.getId());
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
