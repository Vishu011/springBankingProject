package com.bank.aiorchestrator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import feign.RequestInterceptor;

/**
 * Feign configuration to attach OAuth2 client-credentials token to outbound requests.
 * Uses the client registration 'orchestrator-client' configured in application.yaml.
 */
@Configuration
public class FeignOAuth2Config {

    private static final Logger log = LoggerFactory.getLogger(FeignOAuth2Config.class);
    private static final String CLIENT_REGISTRATION_ID = "orchestrator-client";

    /**
     * Authorized client manager configured for client_credentials flow.
     * For non-web contexts, we wire DefaultOAuth2AuthorizedClientManager with
     * a ClientRegistrationRepository and an OAuth2AuthorizedClientService.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    /**
     * Feign RequestInterceptor that fetches an access token and sets the Authorization header.
     */
    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return requestTemplate -> {
            try {
                // In client_credentials, the "principal" is a synthetic name (no user). Any non-null String works.
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                        .principal("ai-orchestrator-service")
                        .build();

                OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
                if (authorizedClient == null) {
                    log.warn("Failed to authorize client '{}' - proceeding without Authorization header",
                            CLIENT_REGISTRATION_ID);
                    return;
                }

                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                if (accessToken != null) {
                    requestTemplate.header("Authorization", "Bearer " + accessToken.getTokenValue());
                } else {
                    log.warn("No access token obtained for client '{}'", CLIENT_REGISTRATION_ID);
                }
            } catch (Exception ex) {
                log.error("OAuth2 token acquisition failed for Feign request: {}", ex.getMessage());
                // Do not throw to avoid breaking calls in DEV; endpoints may still be open locally.
            }
        };
    }
}
