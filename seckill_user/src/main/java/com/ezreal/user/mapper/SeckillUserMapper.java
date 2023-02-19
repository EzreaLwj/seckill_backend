package com.ezreal.user.mapper;

import com.ezreal.common.model.domain.SeckillUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Ezreal
* @description 针对表【seckill_user(用户表)】的数据库操作Mapper
* @createDate 2023-01-08 11:08:10
* @Entity com.ezreal.common.model.domain.SeckillUser
*/
@Mapper
public interface SeckillUserMapper extends BaseMapper<SeckillUser> {

}




