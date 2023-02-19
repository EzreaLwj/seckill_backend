package com.ezreal.goods.mapper;
import org.apache.ibatis.annotations.Param;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezreal.common.model.domain.SeckillGood;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Ezreal
* @description 针对表【seckill_good(秒杀品)】的数据库操作Mapper
* @createDate 2023-01-03 00:17:00
* @Entity com.ezreal.common.model.domain.SeckillGood
*/
@Mapper
public interface SeckillGoodMapper extends BaseMapper<SeckillGood> {
    int decreaseAvailableStockById(@Param("id") Long id,
                                   @Param("quantity") Integer quantity,
                                   @Param("oldAvailableStock") Integer oldAvailableStock);

    int increaseAvailableStockById(@Param("id") Long id,
                                   @Param("quantity") Integer quantity,
                                   @Param("oldAvailableStock") Integer oldAvailableStock);

    int selectAvailableStockById(@Param("id") Long id);
}




