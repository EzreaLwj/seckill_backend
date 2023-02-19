package com.ezreal.order.mapper;

import com.ezreal.common.model.domain.SeckillOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Ezreal
* @description 针对表【seckill_order(秒杀订单表)】的数据库操作Mapper
* @createDate 2023-01-06 13:13:56
* @Entity com.ezreal.common.model.domain.SeckillOrder
*/
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

}




