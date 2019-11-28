/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static java.util.Collections.emptySet;
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

        HstSiteMapItem siteMapItem = createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.isAuthenticated()).andReturn(false).anyTimes();
        expect(siteMapItem.getParentItem()).andReturn(null).anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);

        replay(siteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);
        
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


        HstSiteMapItem siteMapItem = createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(siteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(siteMapItem.getUsers()).andReturn(users).anyTimes();
        expect(siteMapItem.getParentItem()).andReturn(null).anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(users).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);


        replay(siteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);
        
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

        HstSiteMapItem siteMapItem = createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(siteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(siteMapItem.getUsers()).andReturn(users).anyTimes();
        expect(siteMapItem.getParentItem()).andReturn(null).anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(users).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);

        replay(siteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);
        
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

        HstSiteMapItem siteMapItem = createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(siteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(siteMapItem.getUsers()).andReturn(users).anyTimes();
        expect(siteMapItem.getParentItem()).andReturn(null).anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.isAuthenticated()).andReturn(true).anyTimes();
        
        expect(resolvedMount.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(users).anyTimes();
        
        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();

        replay(siteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);
        
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
    public void testCheckAuthorizedByUsersOnMount() throws Exception {
        Set<String> roles = new HashSet<String>();
        
        Set<String> users = new HashSet<String>();
        CollectionUtils.addAll(users, StringUtils.split("alpha bravo"));

        HstSiteMapItem siteMapItem = createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.isAuthenticated()).andReturn(false).anyTimes();
        expect(siteMapItem.getRoles()).andReturn(roles).anyTimes();
        expect(siteMapItem.getUsers()).andReturn(users).anyTimes();
        expect(siteMapItem.getParentItem()).andReturn(null).anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(siteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.isAuthenticated()).andReturn(true).anyTimes();

        expect(resolvedMount.getRoles()).andReturn(roles).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(users).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();

        replay(siteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);

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


    @Test
    public void testCheckAuthorized_hierarchical_by_users_and_roles() throws Exception {

        HstSiteMapItem parentSiteMapItem = createNiceMock(HstSiteMapItem.class);

        expect(parentSiteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(parentSiteMapItem.getRoles()).andReturn(setOf(new String[]{"uberstaff"})).anyTimes();
        expect(parentSiteMapItem.getUsers()).andReturn(emptySet()).anyTimes();
        expect(parentSiteMapItem.getParentItem()).andReturn(null).anyTimes();

        HstSiteMapItem childSiteMapItem = createNiceMock(HstSiteMapItem.class);

        expect(childSiteMapItem.isAuthenticated()).andReturn(true).anyTimes();
        expect(childSiteMapItem.getRoles()).andReturn(setOf(new String[]{"superstaff"})).anyTimes();
        expect(childSiteMapItem.getUsers()).andReturn(emptySet()).anyTimes();
        expect(childSiteMapItem.getParentItem()).andReturn(parentSiteMapItem).anyTimes();
        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);

        expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(childSiteMapItem).anyTimes();

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.isAuthenticated()).andReturn(true).anyTimes();

        expect(resolvedMount.getRoles()).andReturn(emptySet()).anyTimes();
        expect(resolvedMount.getUsers()).andReturn(setOf(new String[]{"charley"})).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();

        replay(parentSiteMapItem, childSiteMapItem, resolvedMount, resolvedSiteMapItem, requestContext);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);

        servletRequest.setUserPrincipal(userPrincipal);

        try {
            securityValve.checkAccess(servletRequest);
            fail("User not in right role so should not have access");
        } catch (ContainerSecurityException securityEx) {
        }

        servletRequest.addUserRole("superstaff");

        try {
            securityValve.checkAccess(servletRequest);
            fail("User does not yet have the parent item role 'uberstaff' so should not have access yet");
        } catch (ContainerSecurityException securityEx) {
        }

        servletRequest.addUserRole("uberstaff");

        try {
            securityValve.checkAccess(servletRequest);
        } catch (ContainerSecurityException securityEx) {
            fail("User should pass all sitemap items since has role 'uberstaff' and 'superstaff' and is user 'charley' " +
                    "which should pass the mount");
        }
    }

    private Set<String> setOf(final String[] strings) {
        return Arrays.stream(strings).collect(Collectors.toSet());
    }

}
