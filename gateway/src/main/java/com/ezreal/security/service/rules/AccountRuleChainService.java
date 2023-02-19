package com.ezreal.security.service.rules;

import com.ezreal.security.model.Rule;
import com.ezreal.security.service.SecurityRuleChainService;
import com.ezreal.security.service.SecurityRuleChainServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {
    private static final Logger logger = LoggerFactory.getLogger(AccountRuleChainService.class);

    @Override
    public boolean run(ServerHttpRequest request, ServerHttpResponse response) {
        Rule rule = securityRulesConfigurationComponent.getAccountRule();
        if (!rule.isEnable()) {
            return true;
        }
        try {
            // 可在此处调用大数据接口或黑名单接口验证账号
            return true;
        } catch (Exception e) {
            logger.error("accountLimit|IP限制异常|", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String getName() {
        return "账号安全服务";
    }
}
