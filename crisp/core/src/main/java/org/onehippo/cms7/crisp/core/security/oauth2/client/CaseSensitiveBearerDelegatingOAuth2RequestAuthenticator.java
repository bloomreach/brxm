/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.security.oauth2.client;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

/**
 * Delegating {@code OAuth2RequestAuthenticator} to inspect the <code>Authorization</code> header value
 * starting with "bearer ..." to correct to "Bearer ..." to be safer with any OAuth2 server implementations.
 * @see <a href="https://issues.onehippo.com/browse/CRISP-7">https://issues.onehippo.com/browse/CRISP-7</a>
 */
public class CaseSensitiveBearerDelegatingOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator {

    private static final String LOWER_CASED_BEARER_PREFIX = "bearer ";
    private static final int LOWER_CASED_BEARER_PREFIX_LEN = LOWER_CASED_BEARER_PREFIX.length();
    private static final String CASE_SENSITIVE_BEARER_PREFIX = "Bearer ";

    private final OAuth2RequestAuthenticator delegate;

    public CaseSensitiveBearerDelegatingOAuth2RequestAuthenticator(final OAuth2RequestAuthenticator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void authenticate(OAuth2ProtectedResourceDetails resource, OAuth2ClientContext clientContext,
            ClientHttpRequest request) {
        delegate.authenticate(resource, clientContext, request);

        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith(LOWER_CASED_BEARER_PREFIX)) {
            authHeader = CASE_SENSITIVE_BEARER_PREFIX + authHeader.substring(LOWER_CASED_BEARER_PREFIX_LEN);
            request.getHeaders().set("Authorization", authHeader);
        }
    }

}
