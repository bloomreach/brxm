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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CopyNodeTest extends RepositoryTestCase {

    @Test
    public void testVirtualTreeCopy() throws RepositoryException {
        Node node, root = session.getRootNode().addNode("test","nt:unstructured");
        root.addMixin("mix:referenceable");
        node = root.addNode("documents");
        node = node.addNode("document","hippo:testdocument");
        node.addMixin("mix:versionable");
        node.setProperty("aap", "noot");
        session.save();
        node = root.addNode("navigation");
        node = node.addNode("search",HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[0]);
        session.save();

        assertTrue(root.getNode("navigation").getNode("search").hasNode("documents"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("documents").hasNode("document"));

        ((HippoSession)session).copy(root.getNode("navigation"), "/test/copy");
        session.save();
        session.refresh(false);

        root = session.getRootNode().getNode("test");
        assertTrue(root.getNode("copy").getNode("search").hasNode("documents"));
        assertTrue(root.getNode("copy").getNode("search").getNode("documents").hasNode("document"));
    }

    @Test
    public void testMultiValuedPropertyCopied() throws Exception {
        build(new String[] {
                "/test", "nt:unstructured",
                "/test/node", "hippo:testrelaxed",
        }, session);
        session.save();
        session.refresh(false);
        Node node = session.getRootNode().getNode("test/node");
        node.setProperty("string", new Value[0], PropertyType.STRING);
        node.setProperty("double", new Value[0], PropertyType.DOUBLE);
        session.save();
        Node copy = ((HippoSession) session).copy(node, "/test/copy");
        assertEquals(PropertyType.STRING, copy.getProperty("string").getType());
        assertEquals(PropertyType.DOUBLE, copy.getProperty("double").getType());
    }

}
