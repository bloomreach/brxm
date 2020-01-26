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
import javax.servlet.http.HttpSessionBindingListener;

import org.easymock.Capture;
import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.hst.container.security.InvalidTokenException;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestNimbusJwtTokenServiceImpl {

    @Test
    public void testCreateAndVerifySignedToken() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        final HttpSession session = createNiceMock(HttpSession.class);
        expect(request.getSession(eq(false))).andStubReturn(session);
        CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);
        expect(session.getAttribute(CmsSessionContext.SESSION_KEY)).andStubReturn(cmsSessionContext);
        expect(cmsSessionContext.getId()).andReturn("subject");
        replay(request, session, cmsSessionContext);
        final String jwsToken = tokenService.createToken(request, Collections.emptyMap());
        final AccessToken accessToken = tokenService.getAccessToken(jwsToken);
        assertEquals("subject", accessToken.getSubject());
    }

    @Test
    public void testTokenWithCustomClaims() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        final HttpSession session = createNiceMock(HttpSession.class);
        expect(request.getSession(eq(false))).andStubReturn(session);
        CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);
        expect(session.getAttribute(CmsSessionContext.SESSION_KEY)).andStubReturn(cmsSessionContext);
        expect(cmsSessionContext.getId()).andReturn("subject");
        replay(request, session, cmsSessionContext);
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("foo", "bar");
        claims.put("bar", "foo");
        // {"sub": "random"} claim will be ignored/overwritten as it is predefined and used internally
        claims.put("sub", "random");
        final String jwsToken = tokenService.createToken(request,claims);
        final AccessToken accessToken = tokenService.getAccessToken(jwsToken);
        assertEquals("subject", accessToken.getSubject());
        assertEquals("foo", accessToken.getClaim("bar"));
        assertEquals("bar", accessToken.getClaim("foo"));
        assertEquals("subject", accessToken.getClaim("sub"));
    }

    @Test
    public void testNoLongerValidToken() {
        NimbusJwtTokenServiceImpl tokenService = new NimbusJwtTokenServiceImpl();
        tokenService.init();
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        final HttpSession session = createNiceMock(HttpSession.class);
        expect(request.getSession(eq(false))).andStubReturn(session);
        CmsSessionContext cmsSessionContext = createNiceMock(CmsSessionContext.class);
        expect(session.getAttribute(CmsSessionContext.SESSION_KEY)).andStubReturn(cmsSessionContext);
        expect(cmsSessionContext.getId()).andReturn("subject");
        Capture<HttpSessionBindingListener> sessionBindingListenerCapture = Capture.newInstance();
        session.setAttribute(anyString(), capture(sessionBindingListenerCapture));
        expectLastCall().anyTimes();
        replay(request, session, cmsSessionContext);
        final String jwsToken = tokenService.createToken(request, Collections.emptyMap());
        // should succeed
        tokenService.getAccessToken(jwsToken);

        // invalidate cmsSessionToken
        sessionBindingListenerCapture.getValue().valueUnbound(null);
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
        }
    }
}
