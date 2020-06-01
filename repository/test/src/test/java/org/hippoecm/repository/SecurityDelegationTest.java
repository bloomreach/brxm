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
package org.hippoecm.repository;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SecurityDelegationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode("bob")) {
            final Node ion = users.addNode("bob", "hipposys:user");
            ion.setProperty("hipposys:password", "bob");
            final Node alice = users.addNode("alice", "hipposys:user");
            alice.setProperty("hipposys:password", "alice");
        }

        final Node root = session.getRootNode();
        if (!root.hasNode("test")) {
            final Node test = root.addNode("test");
            final Node wonderland = test.addNode("wonderland", "hippo:authtestdocument");
            wonderland.setProperty("creator", "carroll");
            wonderland.setProperty("type", "novel");
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (!domains.hasNode("alicesdomain")) {
            // alice has access to wonderland because it's created by carroll
            final Node alicesdomain = domains.addNode("alicesdomain", "hipposys:domain");
            final Node institutions = alicesdomain.addNode("books", "hipposys:domainrule");
            final Node includeAssembly = institutions.addNode("include-carrolls-creation", "hipposys:facetrule");
            includeAssembly.setProperty("hipposys:equals", true);
            includeAssembly.setProperty("hipposys:facet", "creator");
            includeAssembly.setProperty("hipposys:type", "String");
            includeAssembly.setProperty("hipposys:value", "carroll");
            final Node aliceisadmin = alicesdomain.addNode("aliceisadmin", "hipposys:authrole");
            aliceisadmin.setProperty("hipposys:users", new String[]{"alice"});
            aliceisadmin.setProperty("hipposys:role", "admin");
        }
        if (!domains.hasNode("bobsdomain")) {
            final Node bobsdomain = domains.addNode("bobsdomain", "hipposys:domain");
            final Node bobisadmin = bobsdomain.addNode("bobisadmin", "hipposys:authrole");
            bobisadmin.setProperty("hipposys:users", new String[]{"bob"});
            bobisadmin.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (users.hasNode("bob")) {
            users.getNode("bob").remove();
        }
        if (users.hasNode("alice")) {
            users.getNode("alice").remove();
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (domains.hasNode("alicesdomain")) {
            domains.getNode("alicesdomain").remove();
        }
        if (domains.hasNode("bobsdomain")) {
            domains.getNode("bobsdomain").remove();
        }

        session.save();
        super.tearDown();
    }

    /**
     * Sanity test that configuration setup is correct
     */
    @Test
    public void aliceCanAccessWonderlandButBobCannot() throws Exception {
        final Session alice = session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final Node wonderland = alice.getNode("/test/wonderland");
        wonderland.setProperty("members", "alice, etc.");
        alice.save();

        final Session ion = session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        assertFalse(ion.nodeExists("/test/wonderland"));
    }

    /**
     * Sanity test that non delegated searches are correct
     */
    @Test
    public void aliceCanSearchWonderlandButBobCannot() throws Exception {
        final Session alice = session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final Node wonderland = alice.getNode("/test/wonderland");
        wonderland.setProperty("members", "alice, etc.");
        alice.save();
        String xpath = "/jcr:root" + wonderland.getPath();
        assertEquals(1L, alice.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());

        final Session bob = session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        assertFalse(bob.nodeExists("/test/wonderland"));

        assertEquals(0L, bob.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());
    }


    @Test
    public void aliceDelegatesWonderlandAccessToBob() throws Exception {
        final HippoSession bob = (HippoSession) session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        assertFalse(bob.nodeExists("/test/wonderland"));

        final Session alice = session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        assertTrue(alice.nodeExists("/test/wonderland"));

        final Session testSession = bob.createSecurityDelegate(alice);
        // bob can get access to wonderland in the new session
        assertTrue(testSession.nodeExists("/test/wonderland"));
        final Node wonderland = testSession.getNode("/test/wonderland");
        wonderland.setProperty("members", "bob, etc.");
        testSession.save();
    }

    @Test
    public void aliceDelegatesWonderlandSearchAccessToBob() throws Exception {
        final HippoSession bob = (HippoSession) session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        String xpath = "/jcr:root/test/wonderland";
        assertEquals(0L, bob.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());

        final Session alice = session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        assertEquals(1L, alice.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());

        final Session testSession = bob.createSecurityDelegate(alice);
        // bob can search wonderland in the new session
        assertEquals(1L, testSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());
    }


    @Test
    public void aliceCanBeRevokedWonderlandAccessByProgrammaticDomainRuleExtension() throws Exception {
        // exclude /test/wonderland from alices domain by adding a facet rule to the existing institutions domain rule
        // that contradicts the creator property on the wonderland node
        final FacetRule facetRule = new FacetRule("type", "poem", true, false, PropertyType.STRING);
        final DomainRuleExtension domainRuleExtension = new DomainRuleExtension("alicesdomain", "books", Arrays.asList(facetRule));

        final HippoSession alice = (HippoSession) session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final HippoSession bob = (HippoSession)session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        final Session testSession = bob.createSecurityDelegate(alice, domainRuleExtension);
        assertFalse(testSession.nodeExists("/test/wonderland"));
    }

    @Test
    public void aliceSearchCanBeRevokedWonderlandAccessByProgrammaticDomainRuleExtension() throws Exception {
        // exclude /test/wonderland from alices domain by adding a facet rule to the existing institutions domain rule
        // that contradicts the creator property on the wonderland node
        final FacetRule facetRule = new FacetRule("type", "poem", true, false, PropertyType.STRING);
        final DomainRuleExtension domainRuleExtension = new DomainRuleExtension("alicesdomain", "books", Arrays.asList(facetRule));

        final HippoSession alice = (HippoSession) session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final Session bob = session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        final Session testSession = alice.createSecurityDelegate(bob, domainRuleExtension);
        String xpath = "/jcr:root/test/wonderland";
        assertEquals(0L, testSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute().getNodes().getSize());
    }

    @Test
    public void sessionDelegationIsSymmetricWrtAccess() throws Exception {
        final HippoSession alice = (HippoSession) session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final HippoSession bob = (HippoSession)session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        Session delegateOne = alice.createSecurityDelegate(bob);
        Session delegateTwo = bob.createSecurityDelegate(alice);
        checkAccess(delegateOne.getRootNode(), delegateTwo);
        checkAccess(delegateTwo.getRootNode(), delegateOne);
    }

    private void checkAccess(final Node node, final Session other) throws RepositoryException {
        assertTrue(other.itemExists(node.getPath()));
        for (Node child :new NodeIterable(node.getNodes())) {
            if (!((HippoNode)child).isVirtual()) {
                checkAccess(child, other);
            }
        }
    }

    @Test
    public void sessionDelegationUserIdSortedConcatenated() throws Exception {
        final HippoSession alice = (HippoSession) session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final HippoSession bob = (HippoSession)session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        Session delegateOne = alice.createSecurityDelegate(bob);
        assertEquals(alice.getUserID()+","+bob.getUserID(), delegateOne.getUserID());
        Session delegateTwo = bob.createSecurityDelegate(alice);
        assertTrue(delegateOne.getUserID().equals(delegateTwo.getUserID()));
    }

    @Test
    public void domainRuleExtensionNotReflectedInUserId() throws Exception {
        final FacetRule facetRule = new FacetRule("type", "poem", true, false, PropertyType.STRING);
        final DomainRuleExtension domainRuleExtension = new DomainRuleExtension("alicesdomain", "books", Arrays.asList(facetRule));
        final HippoSession alice = (HippoSession) session.getRepository().login(new SimpleCredentials("alice", "alice".toCharArray()));
        final Session bob = session.getRepository().login(new SimpleCredentials("bob", "bob".toCharArray()));
        final Session testSession = alice.createSecurityDelegate(bob, domainRuleExtension);
        assertEquals(alice.getUserID()+","+bob.getUserID(), testSession.getUserID());
    }


}
