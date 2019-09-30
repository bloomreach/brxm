/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferenceJcrPathAuthorizationImplicitReadTest extends AbstractReferenceJcrPathAuthorization {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        removeDefaultReadForTestAndDescendants();

        // create users
        createUser("bob");

        // create content
        final Node root = session.getRootNode();

        final Node test = root.addNode("test", "hippostd:folder");

        final Node folder = test.addNode("folder", "hippostd:folder");
        folder.setProperty("hippostd:foldertype", new String[]{"foo", "bar"});


        final Node sub1 = folder.addNode("sub1", "hippostd:folder");
        final Node sub2 = folder.addNode("sub2", "hippostd:folder");

        sub1.addNode("sub1sub", "hippostd:folder");
        sub2.addNode("sub2sub", "hippostd:folder");

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/bob");
        removeNode("/hippo:configuration/hippo:domains/pathFacetRuleDomain");
        restoreDefaultReadForTestAndDescendants();
        session.save();
        super.tearDown();
    }

    @Test
    public void implicit_read_access_to_ancestry_when_a_descendant_has_path_reference_access() throws Exception {

        assertNoReadAccess("/test", "bob");

        // set up authorization rules
        setAdminRoleOn("/test/folder/sub1", "bob", true);

        session.save();

        Session bob = null;
        try {
            bob = loginUser("bob");

            assertTrue(bob.nodeExists("/test"));
            assertTrue(bob.nodeExists("/test/folder"));

            assertThat(isAllowed("/test/folder", "jcr:read", bob))
                    .as("bob should have implicit read access")
                    .isTrue();

            assertThat(isAllowed("/test/folder", "jcr:write", bob))
                    .as("bob should not have implicit write access")
                    .isFalse();


            bob.checkPermission("/test/folder/sub1", "jcr:read");
            bob.checkPermission("/test/folder/sub1", "jcr:write");

            bob.checkPermission("/test/folder/sub1/sub1sub", "jcr:read");
            bob.checkPermission("/test/folder/sub1/sub1sub", "jcr:write");

            assertThat(isAllowed("/test/folder/sub2", "jcr:read", bob))
                    .as("bob should not have read access on sub2")
                    .isFalse();

        } finally {
            if (bob != null) {
                bob.logout();
            }
        }
    }

    /**
     * When implicit read access is given to some ancestry of a certain path, this should be accounted for in the
     * authorization query. For example when implicit read access is acquired on 'test' and 'folder'
     * via jcr:path=/test/folder/sub1, then if the authorization query does not account for 'test' and 'folder' in its
     * 'read cached bit set', then a path query on say "/jcr:root/test/folder/sub1" won't return any results since
     * the hierarchy query is evaluated by requiring the authorization query to have access on 'test' and 'folder'
     */
    @Test
    public void implicit_read_access_to_ancestry_is_accounted_for_in_authorization_query() throws Exception {

        assertNoReadAccess("/test", "bob");

        // set up authorization rules
        setAdminRoleOn("/test/folder/sub1", "bob", true);
        session.save();

        Session bob = null;
        try {
            bob = loginUser("bob");

            assertTrue(bob.nodeExists("/test"));


            final String xpaths[] = {
                    "/jcr:root/test",
                    "/jcr:root/test/folder",
                    "/jcr:root/test/folder/sub1",
                    "/jcr:root/test/folder/sub1/sub1sub",
                    "/jcr:root/test/folder/sub1//*",
                    "//element(test, hippostd:folder)",
                    "//element(folder, hippostd:folder)",
                    "//element(sub1, hippostd:folder)",
                    "//element(sub1sub, hippostd:folder)"
            };

            for (String xpath : xpaths) {
                final Query query = bob.getWorkspace().getQueryManager()
                        .createQuery(xpath, "xpath");
                final QueryResult result = query.execute();

                assertThat(result.getNodes().getSize())
                        .as(String.format("expected to find 1 result but we didn't for query '%s'", xpath))
                        .isEqualTo(1);
            }
        } finally {
            if (bob != null) {
                bob.logout();
            }
        }

    }

    /**
     * In general, when there is a path reference below which (partly) all nodes match, then the ancestry should get
     * implicit read access. But if there is a sibling facet-rule which in turn *excludes* the referenced node, then
     * the ancestry should not get read-access : Since facet-rules are AND-ed, and if the second facet-rule always
     * returns 'false' for all nodes, then obviously, the ancestry should not get implicit read access since this would
     * violate the model that facet-rules are AND-ed and that true & false should not result in added implicit ancestry
     * read access
     */
    @Test
    public void no_implicit_read_access_on_ancestry_when_other_facet_rules_exclude_the_referenced_jcr_path() throws Exception {

        // set up authorization rules
        setAdminRoleOn("/test/folder/sub1", "bob", true);

        final Node domainRule = session.getNode("/hippo:configuration/hippo:domains/pathFacetRuleDomain/all-nodes-below-jcrPath");

        // to the domain rule, add a facet rule that excludes all nodes including the node that is referenced by the
        // "jcr:path" reference facet ruls
        Node facetRule = domainRule.addNode("exclude-all-non-red", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "color");
        facetRule.setProperty("hipposys:type", "String");
        facetRule.setProperty("hipposys:value", "red");
        // since filter is false (missing), every node that matches must have  'color=red' which is any node!

        session.save();

        assertNoReadAccess("/test", "bob");
    }

    @Ignore("See REPO-2212")
    @Test
    public void deny_path_access_does_not_give_implicit_read_access_to_ancestors() throws Exception {
        // set up authorization rules, give admin role but with 'equals' set to false for path "/test/folder/sub1"

        setAdminRoleOn("/test/folder/sub1", "bob", false);

        session.save();

        assertNoReadAccess("/test", "bob");
    }

    private boolean isAllowed(final String absPath, final String action, final Session user) throws RepositoryException {
        try {
            user.checkPermission(absPath, action);
            return true;
        } catch (java.security.AccessControlException e) {
            return false;
        }
    }

    private void setAdminRoleOn(final String jcrPath, final String userId, final boolean equals) throws RepositoryException {
        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        // bob can read and write to /test/folder/sub1 and everything *below*
        final Node pathFacetRuleDomain = domains.addNode("pathFacetRuleDomain", "hipposys:domain");
        Node domainRule = pathFacetRuleDomain.addNode("all-nodes-below-jcrPath", "hipposys:domainrule");
        Node facetRule = domainRule.addNode("allow-by-path", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", equals);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", jcrPath);
        createAdminAuthRole(pathFacetRuleDomain, userId);
    }


    private void assertNoReadAccess(final String path, final String userId) throws RepositoryException {
        Session user = null;
        try {
            user = loginUser(userId);

            assertFalse(user.nodeExists(path));

        } finally {
            if (user != null) {
                user.logout();
            }
        }
    }


}
