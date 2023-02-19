package com.ezreal.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.model.builder.SeckillUserBuilder;
import com.ezreal.common.model.domain.SeckillUser;
import com.ezreal.common.model.enums.MessageCodeStatus;
import com.ezreal.common.model.enums.SeckillUserStatus;
import com.ezreal.common.model.request.MessageCodeRequest;
import com.ezreal.common.model.request.user.UserLoginRequest;
import com.ezreal.common.model.request.user.UserRegisterRequest;
import com.ezreal.common.model.response.user.UserLoginResponse;
import com.ezreal.common.model.response.user.UserRegisterResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.user.model.MessageCode;
import com.ezreal.user.service.SeckillUserService;
import com.ezreal.user.mapper.SeckillUserMapper;
import com.ezreal.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.ezreal.common.constant.CacheConstant.USER_LOGIN_MESSAGE_CODE_KEY;
import static com.ezreal.common.constant.CacheConstant.USER_REGISTER_MESSAGE_CODE_KEY;

/**
* @author Ezreal
* @description 针对表【seckill_user(用户表)】的数据库操作Service实现
* @createDate 2023-01-08 11:08:10
*/
@Service
@Slf4j
public class SeckillUserServiceImpl extends ServiceImpl<SeckillUserMapper, SeckillUser>
    implements SeckillUserService{

    private static final Logger logger = LoggerFactory.getLogger(SeckillUserServiceImpl.class);

    @Autowired
    private SeckillUserMapper seckillUserMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final static String USER_ID = "user_id";
    private final static String USER_STATUS = "user_status";
    private final static String SALT = "ezreal";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24 * 3;

    @Override
    public BaseResponse<UserLoginResponse> userLogin(UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null || !userLoginRequest.validate()) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }
        String phone = userLoginRequest.getPhone();

        LambdaQueryWrapper<SeckillUser> seckillUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        seckillUserLambdaQueryWrapper.eq(SeckillUser::getPhone, phone);
        SeckillUser seckillUser = seckillUserMapper.selectOne(seckillUserLambdaQueryWrapper);

        if (seckillUser == null) {
            logger.info("用户未找到|{}", JSON.toJSONString(userLoginRequest));
            return ResultUtils.error(ErrorCode.USER_NOT_FOUNT);
        }
        String messageCode = userLoginRequest.getMessageCode();

        String messageCodeInRedis = (String) redisTemplate.opsForValue().get(getMessageCodeKey(phone, MessageCodeStatus.Login.getStatus()));

        if (messageCodeInRedis == null || !messageCodeInRedis.equals(messageCode)) {
            logger.info("验证码错误|{}, {}", seckillUser.getId(), JSON.toJSONString(userLoginRequest));
            return ResultUtils.error(ErrorCode.MESSAGE_CODE_LOGIN_ERROR);
        }


        String token = getToken(seckillUser.getId(), seckillUser.getStatus());
        UserLoginResponse userLoginResponse = SeckillUserBuilder.toLoginResponse(seckillUser)
                .setToken(token);
        return ResultUtils.success(userLoginResponse);
    }

    @Override
    public BaseResponse<MessageCode> getMessageCode(MessageCodeRequest messageCodeRequest) {

        if (!messageCodeRequest.validate()) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }
        String phone = messageCodeRequest.getPhone();
        Integer status = messageCodeRequest.getStatus();
        String messageCodeKey = getMessageCodeKey(phone, status);

        if (messageCodeKey == null) {
            logger.info("获取验证码身份错误|{}", messageCodeRequest.getStatus());
            throw new BusinessException(ErrorCode.MESSAGE_CODE_STATUS_ERROR);
        }
        Object messageCodeInRedis = redisTemplate.opsForValue().get(messageCodeKey);
        if (messageCodeInRedis != null) {
            logger.info("短信验证码已经存在|{}", phone);
            throw new BusinessException(ErrorCode.MESSAGE_CODE_EXIST);
        }

        String code = ValidateCodeUtils.generateValidateCode4String(6);

        redisTemplate.opsForValue().set(messageCodeKey, code, 1, TimeUnit.MINUTES);
        MessageCode messageCode = new MessageCode()
                .setCode(code)
                .setEndTime(new Date(System.currentTimeMillis() + 1000 * 60));
        return ResultUtils.success(messageCode);
    }

    @Override
    public BaseResponse<UserRegisterResponse> userRegister(UserRegisterRequest userRegisterRequest) {

        if (!userRegisterRequest.validate()) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }

        String messageCodeInRedis = (String) redisTemplate.opsForValue().get(getMessageCodeKey(userRegisterRequest.getPhone(),
                MessageCodeStatus.Register.getStatus()));

        if (messageCodeInRedis == null || !messageCodeInRedis.equals(userRegisterRequest.getMessageCode())) {
            logger.info("注册验证码错误|{}", userRegisterRequest.getMessageCode());
            return ResultUtils.error(ErrorCode.MESSAGE_CODE_REGISTER_ERROR);
        }


        LambdaQueryWrapper<SeckillUser> seckillUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        seckillUserLambdaQueryWrapper.eq(SeckillUser::getPhone, userRegisterRequest.getPhone());
        SeckillUser seckillUser = seckillUserMapper.selectOne(seckillUserLambdaQueryWrapper);
        if (seckillUser != null) {
            logger.info("用户已经存在，防止重复注册|{}", seckillUser.getId());
            return ResultUtils.error(ErrorCode.USER_EXIST);
        }
        // 注册前逻辑处理
        String digestPassword = DigestUtils.md5DigestAsHex(userRegisterRequest.getPassword().getBytes(StandardCharsets.UTF_8));
        userRegisterRequest.setPassword(digestPassword);

        if (StringUtils.isEmpty(userRegisterRequest.getNickName())) {
            String nickName = ValidateCodeUtils.generateValidateCode4String(13);
            userRegisterRequest.setPassword(nickName);
        }
        seckillUser = SeckillUserBuilder.toDomain(userRegisterRequest);
        seckillUser.setStatus(SeckillUserStatus.COMMON_USER.getCode());

        seckillUserMapper.insert(seckillUser);
        logger.info("用户注册成功|{},{}", seckillUser.getId(), seckillUser.getPhone());

        String token = getToken(seckillUser.getId(), SeckillUserStatus.COMMON_USER.getCode());
        UserRegisterResponse userRegisterResponse = SeckillUserBuilder.toRegisterResponse(seckillUser)
                .setToken(token);

        return ResultUtils.success(userRegisterResponse);
    }


    public String getToken(Long userId, Integer status) {
        if (userId == null) {
            return null;
        }
        return JWT.create()
                .withClaim(USER_ID, userId)
                .withClaim(USER_STATUS, status)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SALT));
    }

    public String getMessageCodeKey(String phone, Integer status) {
        if (status.equals(MessageCodeStatus.Login.getStatus())) {
            return USER_LOGIN_MESSAGE_CODE_KEY + phone;
        } else if (status.equals(MessageCodeStatus.Register.getStatus())) {
            return USER_REGISTER_MESSAGE_CODE_KEY + phone;
        }

        return null;
    }
}