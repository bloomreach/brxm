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

import java.util.Collections;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.container.security.InvalidTokenException;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.springframework.mock.web.MockHttpSession;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TestNimbusJwtTokenServiceImpl {

    @Test
    public void testCreateAndVerifySignedToken() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        try {
            HttpServletRequest request = createNiceMock(HttpServletRequest.class);
            final HttpSession session = new MockHttpSession();
            expect(request.getSession(eq(false))).andStubReturn(session);
            CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);
            session.setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
            expect(cmsSessionContext.getId()).andReturn("subject");
            replay(request, cmsSessionContext);
            final String jwsToken = tokenService.createToken(request, Collections.emptyMap());
            final AccessToken accessToken = tokenService.getAccessToken(jwsToken);
            assertEquals("subject", accessToken.getSubject());
            assertNotNull(tokenService.registry.getCmsSessionContext("subject"));
        } finally {
            tokenService.destroy();
        }
    }

    @Test
    public void testTokenWithCustomClaims() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        try {
            HttpServletRequest request = createNiceMock(HttpServletRequest.class);
            final HttpSession session = new MockHttpSession();
            expect(request.getSession(eq(false))).andStubReturn(session);
            CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);
            session.setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
            expect(cmsSessionContext.getId()).andReturn("subject");
            replay(request, cmsSessionContext);
            HashMap<String, Object> claims = new HashMap<>();
            claims.put("foo", "bar");
            claims.put("bar", "foo");
            // {"sub": "random"} claim will be ignored/overwritten as it is predefined and used internally
            claims.put("sub", "random");
            final String jwsToken = tokenService.createToken(request, claims);
            final AccessToken accessToken = tokenService.getAccessToken(jwsToken);
            assertEquals("subject", accessToken.getSubject());
            assertEquals("foo", accessToken.getClaim("bar"));
            assertEquals("bar", accessToken.getClaim("foo"));
            assertEquals("subject", accessToken.getClaim("sub"));
            assertNotNull(tokenService.registry.getCmsSessionContext("subject"));
        } finally {
            tokenService.destroy();
        }
    }

    @Test
    public void testNoLongerValidToken() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        final HttpSession session = new MockHttpSession();
        expect(request.getSession(eq(false))).andStubReturn(session);
        CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);

        session.setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
        expect(cmsSessionContext.getId()).andReturn("subject");

        replay(request, cmsSessionContext);
        final String jwsToken = tokenService.createToken(request, Collections.emptyMap());
        // should succeed
        tokenService.getAccessToken(jwsToken);


        assertNotNull(tokenService.registry.getCmsSessionContext("subject"));
        // invalidate session (aka cms logout) should result in the jwsToken no longer being valid
        session.invalidate();
        // session.invalidate should have remove 'subject' from the registry
        assertNull(tokenService.registry.getCmsSessionContext("subject"));
        try {
            tokenService.getAccessToken(jwsToken);
            fail("Token expected to be no longer valid");
        } catch (InvalidTokenException e) {
            if (!e.getMessage().startsWith("Token is not bound to a CmsSessionContext (any more)")) {
                throw e;
            }
        }

        // destroy and recreate tokenService and thereby the signing keys
        tokenService.destroy();
        tokenService.init();
        try {
            tokenService.getAccessToken(jwsToken);
            fail("Token expected to be no longer valid");
        } catch (InvalidTokenException e) {
            if (!e.getMessage().startsWith("Token is not valid")) {
                throw e;
            }
        } finally {
            tokenService.destroy();
        }
    }
}
