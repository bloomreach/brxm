/*
 *  Copyright 2008 Hippo.
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

import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hippoecm.repository.api.HippoNodeType;

public class HREPTWO451Test extends TestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIssue() throws RepositoryException {
        Node node, root = session.getRootNode().addNode("test");
        node = root.addNode("documents","nt:unstructured");
        node.addMixin("mix:referenceable");
        node = node.addNode("document","hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippo:testfacet", "aap");
        session.save();
        node = root.addNode("navigation");
        node = node.addNode("search",HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:testfacet" });
        session.save();
        assertTrue(root.getNode("navigation").getNode("search").hasNode("aap"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("aap").getNode("hippo:resultset").hasNode("document"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("hippo:resultset").hasNode("document"));
    }
}
