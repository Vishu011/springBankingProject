package com.bank.aiorchestrator.integrations.account;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.account.dto.AccountResponse;

/**
 * Feign client for AccountMicroservice admin/user endpoints used by the agent.
 * Base URL configured via orchestrator.integrations.accountServiceBaseUrl.
 */
@FeignClient(
        name = "account-service-admin",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface AccountAdminClient {

    @GetMapping(value = "/accounts/user/{userId}", consumes = MediaType.ALL_VALUE)
    List<AccountResponse> getAccountsByUser(@PathVariable("userId") String userId);
}
