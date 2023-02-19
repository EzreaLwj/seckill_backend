package com.ezreal.security.service;

import com.ezreal.security.model.AuthResult;
import com.ezreal.config.SecurityRulesConfigurationComponent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public abstract class SecurityRuleChainServiceBase {
    private static final Logger logger = LoggerFactory.getLogger(SecurityRuleChainServiceBase.class);

    @Autowired
    protected AuthorizationService authorizationService;

    @Resource
    protected SlidingWindowLimitService slidingWindowLimitService;

    @Resource
    protected SecurityRulesConfigurationComponent securityRulesConfigurationComponent;

    @PostConstruct
    public void init() {
        logger.info("securityService|{}已初始化", getName());
    }

    protected Long getUserId(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        AuthResult authResult = authorizationService.auth(token);
        if (authResult.isSuccess()) {
            return authResult.getUserId();
        }
        return null;
    }

    public abstract String getName();
}
