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
package org.hippoecm.hst.platform.security;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.container.security.InvalidTokenException;
import org.hippoecm.hst.container.security.JwtTokenService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtTokenServiceImpl implements JwtTokenService {

    private KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256); //or RS384, RS512, PS256, PS384, PS512, ES256, ES384, ES512

    private TokenCmsSessionContextRegistry registry = new TokenCmsSessionContextRegistry();

    private void init() {
        HippoServiceRegistry.register(this, JwtTokenService.class);
    }

    private void destroy() {
        HippoServiceRegistry.unregister(this, JwtTokenService.class);
    }

    @Override
    public String createToken(final HttpServletRequest request, final Map<String, Object> claims) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        if (cmsSessionContext == null) {
            throw new IllegalStateException("Cannot create jwt token for unauthenticated users");
        }

        final String tokenSubject = cmsSessionContext.getId();

        registry.register(tokenSubject, cmsSessionContext, session);

        final JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(tokenSubject)
                .signWith(keyPair.getPrivate());

        // setting empty claims results in exception hence the empty check
        if (!claims.isEmpty()) {
            jwtBuilder.setClaims(claims);
        }
        includeServerId(request, jwtBuilder);

        return jwtBuilder.compact();
    }

    // TODO SERVERID should not be in the TOKEN but as a separate querystring parameter
    // include the SERVERID if present in the cookie as a header
    private void includeServerId(final HttpServletRequest request, final JwtBuilder jwtBuilder) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<String> serverId = Arrays.stream(cookies).filter(cookie -> "SERVERID".equals(cookie.getName()))
                    .map(cookie -> cookie.getValue()).findFirst();
            if (serverId.isPresent()) {
                jwtBuilder.setHeader(Collections.singletonMap("SERVERID", serverId.get()));
            }
        }
    }

    @Override
    public AccessToken getAccessToken(final String jws) {
        try {
            final Jws<Claims> claimsJws = Jwts.parser().setSigningKey(keyPair.getPublic()).parseClaimsJws(jws);

            CmsSessionContext cmsSessionContext = registry.getCmsSessionContext(claimsJws.getBody().getSubject());
            if (cmsSessionContext == null) {
                throw new InvalidTokenException("Token is not bound to a CmsSessionContext (any more)");
            }
            return new AccessTokenImpl(claimsJws, cmsSessionContext);
        } catch (IllegalArgumentException | InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid Signed JWT token : %s", e.getMessage()));
        }
    }

}
