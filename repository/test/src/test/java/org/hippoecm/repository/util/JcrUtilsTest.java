/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;

import org.junit.Assert;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JcrUtilsTest extends RepositoryTestCase {

    @Test(expected = IllegalArgumentException.class)
    public void copyToDescendantFails() throws Exception {
        final String[] content = new String[] {
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured"
        };
        build(content, session);
        final Node node = session.getNode("/test/node");
        JcrUtils.copy(session, "/test/node", "/test/node/foo");
    }

    @Test
    public void copyToSiblingSucceeds() throws Exception {
        final String[] content = new String[]{
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured"
        };
        build(content, session);
        final Node node = session.getNode("/test/node");
        JcrUtils.copy(session, "/test/node", "/test/foo");
        assertTrue(session.nodeExists("/test/foo"));
    }

    @Test
    public void copyToSiblingWithSameNamePrefixSucceeds() throws Exception {
        final String[] content = new String[]{
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured"
        };
        build(content, session);
        final Node node = session.getNode("/test/node");
        JcrUtils.copy(session, "/test/node", "/test/node-foo");
        assertTrue(session.nodeExists("/test/node-foo"));
    }

    @Test
    public void testCopyNodeWithAutoCreatedChildNode() throws Exception {
        final String[] content = new String[] {
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured"
        };
        build(content, session);
        final Node node = session.getNode("/test/node");
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
        final String[] content = new String[] {
                "/test", "nt:unstructured",
                "/test/node", "nt:unstructured"
        };
        build(content, session);
        final Node node = session.getNode("/test/node");
        node.addMixin("mix:referenceable");
        session.save();

        JcrUtils.copy(session, "/test/node", "/test/copy");

        final Node copy = session.getNode("/test/copy");
        assertTrue(copy.isNodeType("mix:referenceable"));
    }

    @Test
    public void testMultiValuedPropertyCopied() throws Exception {
        build(new String[] {
                "/test", "nt:unstructured",
                "/test/node", "hippo:testrelaxed",
        }, session);
        Node node = session.getNode("/test/node");
        node.setProperty("string", new Value[0], PropertyType.STRING);
        node.setProperty("double", new Value[0], PropertyType.DOUBLE);
        session.save();

        JcrUtils.copy(session, "/test/node", "/test/copy");

        final Node copy = session.getNode("/test/copy");
        Assert.assertEquals(PropertyType.STRING, copy.getProperty("string").getType());
        Assert.assertEquals(PropertyType.DOUBLE, copy.getProperty("double").getType());
    }

}
