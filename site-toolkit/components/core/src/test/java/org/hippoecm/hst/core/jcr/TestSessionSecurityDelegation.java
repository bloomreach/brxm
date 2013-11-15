/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;


import java.util.Arrays;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSessionSecurityDelegation extends RepositoryTestCase {


    private static final String PREVIEW_USER_ID = "previewUser";
    private static final String PREVIEW_USER_PASS = "previewPass";
    private static final String LIVE_USER_ID = "liveUser";
    private static final String LIVE_USER_PASS = "livePass";
    private static final String TEST_GROUP_ID = "testgroup";

    private SessionSecurityDelegationImpl sessionSecurityDelegation;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ModifiableRequestContextProvider.set(new MockHstRequestContext());
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);
        // create test user
        createUserAndGroup(users, groups);
        session.save();
        sessionSecurityDelegation = new SessionSecurityDelegationImpl();
        sessionSecurityDelegation.setSecurityDelegationEnabled(true);
        sessionSecurityDelegation.setRepository(server.getRepository());
        sessionSecurityDelegation.setPreviewCredentials(new SimpleCredentials(PREVIEW_USER_ID, PREVIEW_USER_PASS.toCharArray()));
        sessionSecurityDelegation.setLiveCredentials(new SimpleCredentials(LIVE_USER_ID, LIVE_USER_PASS.toCharArray()));
    }

    @Override
    @After
    public void tearDown() throws  Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);
        cleanupUserAndGroup(users, groups);
        session.save();
        ModifiableRequestContextProvider.clear();
        super.tearDown();
    }


    @Test
    public void assertEqualityDomainRulesExtensions(){
        final FacetRule facetRule1 = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "preview", true, true, PropertyType.STRING);
        final DomainRuleExtension dre1 = new DomainRuleExtension("*", "*", Arrays.asList(facetRule1));

        final FacetRule facetRule2 = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "preview", true, true, PropertyType.STRING);
        final DomainRuleExtension dre2 = new DomainRuleExtension("*", "*", Arrays.asList(facetRule2));

        final FacetRule facetRule3 = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "live", true, true, PropertyType.STRING);
        final DomainRuleExtension dre3 = new DomainRuleExtension("*", "*", Arrays.asList(facetRule3));

        final FacetRule facetRule4 = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "live", true, true, PropertyType.STRING);
        final DomainRuleExtension dre4 = new DomainRuleExtension("test", "test", Arrays.asList(facetRule4));

        assertTrue(dre1.equals(dre2));
        assertTrue(dre1.hashCode() == dre2.hashCode());

        assertFalse(dre1.equals(dre3));
        assertFalse(dre1.hashCode() == dre3.hashCode());

        assertFalse(dre3.equals(dre4));
        assertFalse(dre3.hashCode() == dre4.hashCode());

    }

    @Test(expected = IllegalStateException.class)
    public void securityDelegationDisabled() throws RepositoryException {
        sessionSecurityDelegation.setSecurityDelegationEnabled(false);
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds, "test123");
    }

    @Test(expected = IllegalStateException.class)
    public void securityDelegationFailureWhenNoRequestContext() throws RepositoryException {
        ModifiableRequestContextProvider.set(null);
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds, "test123");
    }

    @Test
    public void sessionSecurityDelegationCachedEntries() throws RepositoryException {
        // since SimpleCredentials does not have a equals or hashCode impl, below test with two Credentials objects
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Credentials creds2 = new SimpleCredentials("admin", "admin".toCharArray());
        final Session live1 = sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds, "test123");
        final Session live2 = sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds, "test123");
        final Session live3 = sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds2, "test123");
        final Session live4 = sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds2, "test123-DIFF");
        final Session preview1 = sessionSecurityDelegation.getOrCreatePreviewSecurityDelegate(creds, "test123");
        final Session preview2 = sessionSecurityDelegation.getOrCreatePreviewSecurityDelegate(creds, "test123");

        assertTrue(live1 == live2);
        assertTrue(live1 == live3);
        assertFalse(live1 == live4);
        assertTrue(preview1 == preview2);
        assertFalse(live1 == preview1);

        sessionSecurityDelegation.cleanupSessionDelegates(RequestContextProvider.get());

        final Session live5 = sessionSecurityDelegation.getOrCreateLiveSecurityDelegate(creds, "test123");
        // after clean up sessions get all logout from sessionSecurityDelegation
        assertFalse(live1 == live5);
        sessionSecurityDelegation.cleanupSessionDelegates(RequestContextProvider.get());
    }


    private void createUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        Node previewUser = users.addNode(PREVIEW_USER_ID, HippoNodeType.NT_USER);
        previewUser.setProperty(HippoNodeType.HIPPO_PASSWORD, PREVIEW_USER_PASS);
        Node liveUser = users.addNode(LIVE_USER_ID, HippoNodeType.NT_USER);
        liveUser.setProperty(HippoNodeType.HIPPO_PASSWORD, LIVE_USER_PASS);

        // create test group with member test
        Node testGroup = groups.addNode(TEST_GROUP_ID, HippoNodeType.NT_GROUP);
        testGroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[] { PREVIEW_USER_ID, LIVE_USER_ID });
    }

    private void cleanupUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        if (users.hasNode(PREVIEW_USER_ID)) {
            users.getNode(PREVIEW_USER_ID).remove();
        }
        if (users.hasNode(LIVE_USER_ID)) {
            users.getNode(LIVE_USER_ID).remove();
        }
        if (groups.hasNode(TEST_GROUP_ID)) {
            groups.getNode(TEST_GROUP_ID).remove();
        }
    }


}
