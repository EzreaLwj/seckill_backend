package com.ezreal.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezreal.common.model.domain.SeckillBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author Ezreal
 * @description 针对表【seckill_bucket(秒杀品库存分桶表)】的数据库操作Mapper
 * @createDate 2023-01-17 21:52:11
 * @Entity com.ezreal.common.model.domain.SeckillBucket
 */
@Mapper
public interface SeckillBucketMapper extends BaseMapper<SeckillBucket> {
    List<SeckillBucket> selectByItemId(@Param("itemId") Long itemId);

    SeckillBucket selectByItemIdAndSerialNo(@Param("itemId") Long itemId, @Param("serialNo") Integer serialNo);

    int insertBatch(@Param("seckillBucketCollection") Collection<SeckillBucket> seckillBucketCollection);

    int updateStatusByItemId(@Param("itemId") Long itemId, @Param("status") Integer status);

    int decreaseBucketStock(@Param("itemId") Long itemId,
                            @Param("serialNo") Integer serialNo,
                            @Param("quantity") Integer quantity,
                            @Param("oldAvailableStocksAmount") Integer oldAvailableStocksAmount);

    int increaseBucketStock(@Param("itemId") Long itemId,
                            @Param("serialNo") Integer serialNo,
                            @Param("quantity") Integer quantity,
                            @Param("oldAvailableStocksAmount") Integer oldAvailableStocksAmount);

}




