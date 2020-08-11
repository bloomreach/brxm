/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.security;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public interface JwtTokenService {

    /**
     * <p>
     *     Returns a signed JWT (JWS) token for an {@link HttpServletRequest} that has been authenticated containing an
     *     {@link HttpSession} that can be retrieved via {@link CmsSessionContext#getContext(HttpSession)}.
     *     If the request has not already been authenticated or does not have its {@link HttpSession} attached to the {@link CmsSessionContext},
     *     an {@link IllegalStateException} is thrown
     * </p>
     * @param request the HttpServletRequest which must have an http session that has been authenticated already
     * @param claims the claims to add to the token, for example which branch to render, if no extra claims, use empty map.
     *               The Objects need to be serializable to json
     * @return a signed JWT token
     * @throws IllegalStateException if the http request does not yet have an authenticated {@link HttpSession}
     * or does not yet have it attached to the {@link CmsSessionContext} via {@link CmsContextService#attachSessionContext(String, HttpSession)}
     */
    String createToken(HttpServletRequest request, Map<String, Object> claims);

    /**
     *
     * @param jws the signed JWT token
     * @return An {@link AccessToken} object
     * @throws InvalidTokenException in case the JWT token is not bound to a valid {@link CmsSessionContext} (any more)
     * @throws TokenException in case the jwt service is not initialized or some unexpected exception happened not being
     *         catched and thrown as a InvalidTokenException
     */
    AccessToken getAccessToken(String jws) throws InvalidTokenException;
}
