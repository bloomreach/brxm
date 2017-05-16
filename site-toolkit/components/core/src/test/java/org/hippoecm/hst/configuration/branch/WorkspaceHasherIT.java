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
package org.hippoecm.hst.configuration.branch;

import java.security.MessageDigest;
import java.util.Calendar;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;

import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_DELETED;
import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_HASH;
import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_UPSTREAM_HASH;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_EDITABLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_HASHABLE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WorkspaceHasherIT extends AbstractTestConfigurations {

    @Test
    public void messageDigest_getInstance_returns_new_digest() throws Exception {
        assertNotSame(MessageDigest.getInstance("MD5"), MessageDigest.getInstance("MD5"));
    }

    @Test
    public void messageDigest_order_does_impact_result() throws Exception {
        MessageDigest m1 = MessageDigest.getInstance("MD5");
        m1.update("foo".getBytes());
        byte[] digest1 = m1.digest("bar".getBytes());

        MessageDigest m2 = MessageDigest.getInstance("MD5");
        m2.update("foo".getBytes());
        byte[] digest2 = m2.digest("bar".getBytes());

        assertArrayEquals(digest1, digest2);

        MessageDigest m3 = MessageDigest.getInstance("MD5");
        m3.update("bar".getBytes());
        byte[] digest3 = m2.digest("foo".getBytes());

        assertFalse(ArrayUtils.isEquals(digest1, digest3));
    }

    @Test
    public void hash_hst_workspace() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());
            Node workspaceNode = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            boolean setUpstreamHash = false;
            hasher.hash(workspaceNode, true, setUpstreamHash);
            recursivelyAssertHashProperties(workspaceNode, setUpstreamHash);

            setUpstreamHash = true;
            hasher.hash(workspaceNode, true, setUpstreamHash);
            recursivelyAssertHashProperties(workspaceNode, setUpstreamHash);
        } finally {
            session.logout();
        }
    }

    private void recursivelyAssertHashProperties(final Node node, final boolean setUpstreamHash) throws RepositoryException {
        assertTrue(node.isNodeType(MIXINTYPE_HST_HASHABLE));
        assertTrue(node.hasProperty(HASHABLE_PROPERTY_HASH));
        if (setUpstreamHash) {
            assertEquals(node.getProperty(HASHABLE_PROPERTY_HASH).getString(), node.getProperty(HASHABLE_PROPERTY_UPSTREAM_HASH).getString());
        } else {
            assertFalse(node.hasProperty(HASHABLE_PROPERTY_UPSTREAM_HASH));
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            recursivelyAssertHashProperties(child, setUpstreamHash);
        }
    }

    @Test
    public void marked_deleted_nodes_contribute_to_hash_their_original_hash() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());
            Node workspaceNode = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            boolean setUpstreamHash = true;
            String preHash = hasher.hash(workspaceNode, true, setUpstreamHash);

            Node homeSiteMapItem = workspaceNode.getNode("hst:sitemap/home");
            homeSiteMapItem.setProperty(HASHABLE_PROPERTY_DELETED, true);

            String reHashAfterMarkedDelete = hasher.hash(workspaceNode, true, setUpstreamHash);

            assertFalse("Since a descendant node is marked deleted, the hash should be different", reHashAfterMarkedDelete.equals(preHash));

            homeSiteMapItem.remove();

            String reHashAfterRemove = hasher.hash(workspaceNode, true, setUpstreamHash);

            assertEquals("Since a node marked deleted should not impact the hash, removing that node should not impact " +
                    "the hash either. ", reHashAfterMarkedDelete, reHashAfterRemove);

        } finally {
            session.logout();
        }
    }

    @Test
    public void marked_deleted_nodes_must_have_upstreamhash_equal_to_hash() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());
            Node workspaceNode = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            boolean setUpstreamHash = true;
            hasher.hash(workspaceNode, true, setUpstreamHash);

            Node homeSiteMapItem = workspaceNode.getNode("hst:sitemap/home");
            homeSiteMapItem.setProperty(HASHABLE_PROPERTY_DELETED, true);
            homeSiteMapItem.setProperty(HASHABLE_PROPERTY_UPSTREAM_HASH, "changed");
            // change the upstreamhash to be different than hash which should not be allowed
            // and which we should find out during a rehash
            try {
                hasher.hash(workspaceNode, true, setUpstreamHash);
                fail("hashing should fail if a marked deleted node contains a different upstreamhash than hash");
            } catch (BranchException e) {
                // expected
            }

        } finally {
            session.logout();
        }
    }

    @Test(expected = BranchException.class)
    public void none_hst_workspace_node_not_allowed() throws Exception {
        Session session = createSession();
        try {
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());
            Node configuration = session.getNode("/hst:hst/hst:configurations/unittestproject");
            hasher.hash(configuration, true, false);
        } finally {
            session.logout();
        }
    }

    @Test
    public void hash_copy_of_hst_workspace_results_in_same_hashes_even_if_name_is_not_workspace_but_upstream() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            Node configuration = session.getNode("/hst:hst/hst:configurations/unittestproject");

            configuration.addMixin(MIXINTYPE_HST_BRANCH);
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_OF, "dummy");
            configuration.setProperty(BRANCH_PROPERTY_BRANCH_ID, "dummy");
            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:workspace",
                    "/hst:hst/hst:configurations/unittestproject/hst:upstream");

            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            hasher.hash(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace"), true, false);
            hasher.hash(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"), true, false);

            recursiveAssertHashEquals(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace"),
                    session.getNode("/hst:hst/hst:configurations/unittestproject/hst:upstream"));
        } finally {
            session.logout();
        }
    }

    private void recursiveAssertHashEquals(final Node source, final Node copy) throws RepositoryException {
        assertEquals(source.getProperty(HASHABLE_PROPERTY_HASH).getString(), copy.getProperty(HASHABLE_PROPERTY_HASH).getString());
        for (Node child : new NodeIterable(source.getNodes())) {
            Node copyChild = copy.getNode(child.getName());
            recursiveAssertHashEquals(child, copyChild);
        }
    }

    @Test
    public void hash_of_workspace_containing_hashes_or_upstream_hashes_results_in_same_hashed_tree() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            String hash = hasher.hash(workspace, true, false);

            workspace.setProperty(HASHABLE_PROPERTY_UPSTREAM_HASH, "dummy-hash");

            // a rehash should result in same hashed tree regardless extra hash properties
            String rehash = hasher.hash(workspace, true, false);

            assertEquals("A rehash for a node tree should not change due to existing hashes or upstream hashes because these " +
                    "should be ignored", hash, rehash);

        } finally {
            session.logout();
        }
    }

    @Test
    public void lockedby_and_lockedon_and_lastmodifiedby_and_lastmodified_dont_influence_hashes() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            String hash = hasher.hash(workspace, true, false);

            workspace.addMixin(MIXINTYPE_HST_EDITABLE);
            workspace.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");
            workspace.setProperty(GENERAL_PROPERTY_LAST_MODIFIED_BY, "admin");
            workspace.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, Calendar.getInstance());

            Node sitemap = workspace.getNode("hst:sitemap");
            sitemap.setProperty(GENERAL_PROPERTY_LOCKED_BY, "admin");
            sitemap.setProperty(GENERAL_PROPERTY_LAST_MODIFIED_BY, "admin");
            sitemap.setProperty(GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
            sitemap.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, Calendar.getInstance());

            // a rehash should result in same hashed tree regardless extra properties
            String rehash = hasher.hash(workspace, true, false);

            assertEquals("A rehash for a node tree should not change due to existing lockedby, lockedon or lastmodifiedby because these " +
                    "should be ignored", hash, rehash);

        } finally {
            session.logout();
        }
    }

    @Test
    public void node_names_influences_hashing_except_the_root_node() throws Exception {
        // whether the 'root' node is hst:workspace or hst:upstream, that does not influence the hashing
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            session.move("/hst:hst/hst:configurations/unittestcommon/hst:templates",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates");

            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            String hash = hasher.hash(workspace, true, false);

            session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates/webpage",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates/newnamedpage");

            String rehash = hasher.hash(workspace, true, false);

            assertFalse("A renamed descendant node should result in a different hash", hash.equals(rehash));
        } finally {
            session.logout();
        }
    }

    @Test
    public void node_properties_influences_hashing() throws Exception {

        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            session.move("/hst:hst/hst:configurations/unittestcommon/hst:templates",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates");

            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            String hash = hasher.hash(workspace, true, false);

            session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/_default_")
                    .setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"foo", "bar", "lux"});

            String rehash = hasher.hash(workspace, true, false);

            assertFalse("A different descendant property should result in a different hash", hash.equals(rehash));
        } finally {
            session.logout();
        }

    }

    @Test
    public void node_ordering_influences_hashing() throws Exception {
        Session session = createSession();
        try {
            createWorkspaceNodes(session);
            session.move("/hst:hst/hst:configurations/unittestcommon/hst:templates",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates");

            NodeHasher hasher = HstServices.getComponentManager().getComponent(WorkspaceHasher.class.getName());

            Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace");
            String hash = hasher.hash(workspace, true, false);

            Node templates = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:templates");

            templates.orderBefore("header", "webpage");

            String rehash = hasher.hash(workspace, true, false);

            assertFalse("The hash for the node tree should change due to reordered child nodes", hash.equals(rehash));
        } finally {
            session.logout();
        }
    }

    private void createWorkspaceNodes(final Session session) throws RepositoryException {
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace", "hst:workspace");
        }
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap");

        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");
    }

    private Session createSession() throws RepositoryException {
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(cred);
    }

}
