package com.ezreal.security.service.rules;

import com.ezreal.security.model.Rule;
import com.ezreal.security.service.SecurityRuleChainService;
import com.ezreal.security.service.SecurityRuleChainServiceBase;
import com.ezreal.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * 资源安全服务
 */
@Component
public class ResourcePathRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {
    private static final Logger logger = LoggerFactory.getLogger(ResourcePathRuleChainService.class);

    @Override
    public boolean run(ServerHttpRequest request, ServerHttpResponse response) {
        Rule pathRule = securityRulesConfigurationComponent.getPathRule(request.getURI().getPath());
        if (!pathRule.isEnable()) {
            return true;
        }
        try {
            Long userId = getUserId(request);
            String userResourcePath = StringUtil.link(userId, request.getURI().getPath());
            boolean isPass = slidingWindowLimitService.pass(userResourcePath, pathRule.getWindowPeriod(), pathRule.getWindowSize());
            if (!isPass) {

                logger.info("resourcePathLimit|资源路径限制|{}", userResourcePath);
                return false;
            }
        } catch (Exception e) {
            logger.error("resourcePathLimit|资源路径限制异常|", e);
            return false;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public String getName() {
        return "资源安全服务";
    }
}
