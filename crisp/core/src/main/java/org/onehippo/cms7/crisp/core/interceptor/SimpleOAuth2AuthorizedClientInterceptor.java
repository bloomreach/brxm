/*
 *  Copyright 2022 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.core.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class SimpleOAuth2AuthorizedClientInterceptor implements ClientHttpRequestInterceptor {

    private ClientRegistrationRepository clientRegistrationRepository;
    private OAuth2AuthorizedClientService authorizedClientService;
    private AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager;
    private String clientRegistrationId;
    private Authentication principal;
    private Map<String, Object> contextAttributesMap;

    public SimpleOAuth2AuthorizedClientInterceptor(ClientRegistrationRepository clientRegistrationRepository) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        if (this.authorizedClientManager == null) {
            final OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                    .clientCredentials()
                    .refreshToken()
                    .password()
                    .authorizationCode()
                    .build();

            this.authorizedClientManager =
                    new AuthorizedClientServiceOAuth2AuthorizedClientManager(this.clientRegistrationRepository, this.authorizedClientService);
            this.authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
            this.authorizedClientManager.setContextAttributesMapper(oAuth2AuthorizeRequest -> contextAttributesMap);
        }

        final OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(this.clientRegistrationId)
                .principal(getPrincipal())
                .build();
        final OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(oAuth2AuthorizeRequest);

        final HttpHeaders headers = request.getHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());

        return execution.execute(request, body);
    }

    public ClientRegistrationRepository getClientRegistrationRepository() {
        return clientRegistrationRepository;
    }

    public void setClientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public OAuth2AuthorizedClientService getAuthorizedClientService() {
        return authorizedClientService;
    }

    public void setAuthorizedClientService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    public AuthorizedClientServiceOAuth2AuthorizedClientManager getAuthorizedClientManager() {
        return authorizedClientManager;
    }

    public void setAuthorizedClientManager(AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getClientRegistrationId() {
        return clientRegistrationId;
    }

    public void setClientRegistrationId(String clientRegistrationId) {
        this.clientRegistrationId = clientRegistrationId;
    }

    public Authentication getPrincipal() {
        if (principal == null) {
            this.principal = new AnonymousAuthenticationToken("anonymous",
                    "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        }
        return principal;
    }

    public void setPrincipal(Authentication principal) {
        this.principal = principal;
    }

    public Map<String, Object> getContextAttributesMap() {
        if (contextAttributesMap == null) {
            return Collections.emptyMap();
        }
        return contextAttributesMap;
    }

    public void setContextAttributesMap(Map<String, Object> contextAttributesMap) {
        this.contextAttributesMap = contextAttributesMap;
    }
}