/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

/**
 * TestSecurityValveImpl
 */
public class TestSecurityValveImpl {
    
    private SecurityValve securityValve;
    private Principal userPrincipal;

    @Before
    public void setUp() throws Exception {
        securityValve = new SecurityValve();
        
        userPrincipal = new Principal() {
            public String getName() {
                return "charley";
            }
        };
    }
    
    @Test
    public void testCheckAuthorizedForNonAuthenticated() throws Exception {
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isAuthenticated()).andReturn(false).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        
        replay(resolvedSiteMapItem);
        replay(requestContext);
        
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        
        try {
            securityValve.checkAccess(servletRequest);
        } catch (ContainerSecurityException securityEx) {
            fail("Security valve shouldn't do security checking.");
        }
    }
    
    @Test
    public void testCheckAuthorizedForNonAuthenticatedAccess() throws Exception {
        Set<String> roles = new HashSet<String>();
        Set<String> users = new HashSet<String>();
        
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(resolvedSiteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedSiteMapItem.getUsers()).andReturn(users).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        
        replay(resolvedSiteMapItem);
        replay(requestContext);
        
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve shouldn't try authorization because no user principal found.");
        } catch (ContainerSecurityException securityEx) {
        }
    }
    
    @Test
    public void testCheckAuthorizedByRoles() throws Exception {
        Set<String> roles = new HashSet<String>();
        CollectionUtils.addAll(roles, StringUtils.split("engineering sales"));
        
        Set<String> users = new HashSet<String>();
        
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(resolvedSiteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedSiteMapItem.getUsers()).andReturn(users).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        
        replay(resolvedSiteMapItem);
        replay(requestContext);
        
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        
        servletRequest.setUserPrincipal(userPrincipal);
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        servletRequest.addUserRole("guest");
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        servletRequest.addUserRole("sales");
        
        try {
            securityValve.checkAccess(servletRequest);
        } catch (ContainerSecurityException securityEx) {
            fail("Security valve doesn't check security properly even though the user is in sales.");
        }
        
    }

    @Test
    public void testCheckAuthorizedByRolesOnMount() throws Exception {
        Set<String> roles = new HashSet<String>();
        CollectionUtils.addAll(roles, StringUtils.split("engineering sales"));
        
        Set<String> users = new HashSet<String>();
        
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isAuthenticated()).andReturn(false).anyTimes();
        
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.isAuthenticated()).andReturn(true).anyTimes();
        
        expect(resolvedMount.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(users).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        
        replay(resolvedSiteMapItem);
        replay(resolvedMount);
        replay(requestContext);
        
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        
        servletRequest.setUserPrincipal(userPrincipal);
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        servletRequest.addUserRole("guest");
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        servletRequest.addUserRole("sales");
        
        try {
            securityValve.checkAccess(servletRequest);
        } catch (ContainerSecurityException securityEx) {
            fail("Security valve doesn't check security properly even though the user is in sales.");
        }
        
    }

    @Test
    public void testCheckAuthorizedByUsers() throws Exception {
        Set<String> roles = new HashSet<String>();
        
        Set<String> users = new HashSet<String>();
        CollectionUtils.addAll(users, StringUtils.split("alpha bravo"));
        
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(resolvedSiteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedSiteMapItem.getUsers()).andReturn(users).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        
        replay(resolvedSiteMapItem);
        replay(requestContext);
        
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        
        servletRequest.setUserPrincipal(userPrincipal);
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        users.add("delta");
        
        try {
            securityValve.checkAccess(servletRequest);
            fail("Security valve doesn't work based on request principal.");
        } catch (ContainerSecurityException securityEx) {
        }
        
        users.add("charley");
        
        try {
            securityValve.checkAccess(servletRequest);
        } catch (ContainerSecurityException securityEx) {
            fail("Security valve doesn't check security properly even though the user is in sales.");
        }
        
    }
    
}
