package com.ezreal.security.service.rules;

import com.ezreal.security.model.Rule;
import com.ezreal.security.service.SecurityRuleChainService;
import com.ezreal.security.service.SecurityRuleChainServiceBase;
import com.ezreal.utils.IPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class IPRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {
    private static final Logger logger = LoggerFactory.getLogger(IPRuleChainService.class);

    @Override
    public boolean run(ServerHttpRequest request, ServerHttpResponse response) {
        Rule ipRule = securityRulesConfigurationComponent.getIpRule();
        if (!ipRule.isEnable()) {
            return true;
        }

        try {
            String clientIp = IPUtil.getIpAddr(request);
            // 计数器限流
            boolean isPass = slidingWindowLimitService.pass(clientIp, ipRule.getWindowPeriod(), ipRule.getWindowSize());
            if (!isPass) {

                logger.info("ipLimit|IP被限制|{}", clientIp);
                return false;
            }
        } catch (Exception e) {
            logger.error("ipLimit|IP限制异常|", e);
            return false;
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return "IP防护服务";
    }
}
