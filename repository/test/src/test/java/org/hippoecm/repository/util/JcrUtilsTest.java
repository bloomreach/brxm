/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.util.Collections;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JcrUtilsTest extends RepositoryTestCase {

    private Node node;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final String[] content = new String[]{
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured",
                "/test/doc", "hippo:document"
        };
        build(content, session);
        node = session.getNode("/test/node");
    }

    @Test
    public void getMultipleStringProperty() throws RepositoryException {
        node.setProperty("testProp", new String[]{"foo", "bar"});

        final String[] prop = JcrUtils.getMultipleStringProperty(node, "testProp", null);
        assertEquals(2, prop.length);
        assertEquals("foo", prop[0]);
        assertEquals("bar", prop[1]);
    }

    @Test
    public void getMultipleStringPropertyDefaultValue() throws RepositoryException {
        final String[] prop = JcrUtils.getMultipleStringProperty(node, "noSuchProperty", new String[]{"defaultValue"});
        assertEquals(1, prop.length);
        assertEquals("defaultValue", prop[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyToDescendantFails() throws Exception {
        JcrUtils.copy(session, "/test/node", "/test/node/foo");
    }

    @Test
    public void copyToSiblingSucceeds() throws Exception {
        JcrUtils.copy(session, "/test/node", "/test/foo");
        assertTrue(session.nodeExists("/test/foo"));
    }

    @Test
    public void copyToSiblingWithSameNamePrefixSucceeds() throws Exception {
        JcrUtils.copy(session, "/test/node", "/test/node-foo");
        assertTrue(session.nodeExists("/test/node-foo"));
    }

    @Test
    public void testCopyNodeWithAutoCreatedChildNode() throws Exception {
        node.addMixin("hippotranslation:translated");
        node.setProperty("hippotranslation:locale", "nl");
        node.setProperty("hippotranslation:id", "1");
        session.save();

        JcrUtils.copy(session, "/test/node", "/test/copy");

        final Node copy = session.getNode("/test/copy");
        assertTrue(copy.isNodeType("hippotranslation:translated"));
        assertEquals("nl", copy.getProperty("hippotranslation:locale").getString());
    }

    @Test
    public void testCopyNodeWithProtectedProperty() throws Exception {
        node.addMixin("mix:referenceable");
        session.save();

        JcrUtils.copy(session, "/test/node", "/test/copy");

        final Node copy = session.getNode("/test/copy");
        assertTrue(copy.isNodeType("mix:referenceable"));
    }

    @Test
    public void testMultiValuedPropertyCopied() throws Exception {
        node.setPrimaryType("hippo:testrelaxed");
        node.setProperty("string", new Value[0], PropertyType.STRING);
        node.setProperty("double", new Value[0], PropertyType.DOUBLE);
        session.save();

        JcrUtils.copy(session, "/test/node", "/test/copy");

        final Node copy = session.getNode("/test/copy");
        assertEquals(PropertyType.STRING, copy.getProperty("string").getType());
        assertEquals(PropertyType.DOUBLE, copy.getProperty("double").getType());
    }

    @Test
    public void testCopyNodeToDescendantDestinationIsIllegal() throws Exception {
        try {
            JcrUtils.copy(session, "/test/node", "/test/node/child");
            fail("Should not be able to copy node to own subpath");
        } catch (IllegalArgumentException expected) {
        }
        try {
            JcrUtils.copy(node, "child", node);
            fail("Should not be able to copy node to same node as destination");
        } catch (IllegalArgumentException expected) {
        }
        try {
            JcrUtils.copy(session.getNode("/test"), "child", node);
            fail("Should not be able to copy node to node that is descendant of source");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCopyNodeByPathIsIllegal() throws Exception {
        try {
            JcrUtils.copy(node, "this/is/a/relative/path/not/a/nodeName", node);
            fail("Should not be able to copy node when destinationName is a path");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("path"));
        }
    }

    @Test
    public void testCopyNodeWithNoMatchingChildNodeDef() throws Exception {
        try {
            JcrUtils.copy(session, "/test/node", "/test/doc/node");
            fail("Should not be able to copy node to node with no-matching child node definition");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().equals("No applicable child node definition"));
        }
    }

    @Test
    public void testCopyNodeToProtectedDestination() throws Exception {
        try {
            Node doc = session.getNode("/test/doc");
            node.addMixin("mix:versionable");
            doc.addMixin("mix:versionable");
            session.save();
            JcrUtils.copy(doc.getBaseVersion(), "foo", node.getVersionHistory());
            fail("Should not be able to copy node to node with no-matching child node definition");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().equals("No applicable child node definition"));
        }
    }

    @Test
    public void testCopyFolderExcludesBranchRelatedMixins() throws RepositoryException {

        final Node doc = session.getNode("/test/doc");
        doc.addMixin(HippoNodeType.NT_HIPPO_VERSION_INFO);
        doc.setProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY, "test");
        doc.setProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY, new String[]{"master", "test"});

        doc.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        doc.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "test");
        doc.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "test");
        session.save();

        final Node test2Node = JcrUtils.copy(session.getNode("/test"), "test2", session.getRootNode());
        session.save();

        final Node srcDoc = session.getNode("/test/doc");
        assertThat(srcDoc.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY), is(true));
        assertThat(srcDoc.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME), is(true));

        final Node copiedDoc = session.getNode("/test2/doc");
        assertThat(copiedDoc.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY), is(false));
        assertThat(copiedDoc.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME), is(false));

        test2Node.remove();
        session.save();
    }

    @Test
    public void testCopyNodeExcludesBranchRelatedMixins() throws RepositoryException {

        final Node doc = session.getNode("/test/doc");
        doc.addMixin(HippoNodeType.NT_HIPPO_VERSION_INFO);
        doc.setProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY, "test");
        doc.setProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY, new String[]{"master", "test"});

        doc.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        doc.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "test");
        doc.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "test");
        session.save();

        final Node doc2 = doc.getParent().addNode("doc2", HippoNodeType.NT_HANDLE);
        JcrUtils.copyTo(doc, doc2);
        session.save();

        final Node srcDoc = session.getNode("/test/doc");
        assertThat(srcDoc.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY), is(true));
        assertThat(srcDoc.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID), is(true));
        assertThat(srcDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME), is(true));

        final Node copiedDoc = session.getNode("/test/doc2");
        assertThat(copiedDoc.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY), is(false));
        assertThat(copiedDoc.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID), is(false));
        assertThat(copiedDoc.hasProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME), is(false));

        copiedDoc.remove();
        session.save();
    }

}
