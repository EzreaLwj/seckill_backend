package com.ezreal.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ezreal.common.model.domain.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Ezreal
* @description 针对表【seckill_activity(秒杀活动表)】的数据库操作Mapper
* @createDate 2023-01-02 10:35:44
*/
@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {


}
