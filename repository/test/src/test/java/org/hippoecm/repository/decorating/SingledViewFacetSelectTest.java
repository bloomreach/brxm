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
package org.hippoecm.repository.decorating;

import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

public class SingledViewFacetSelectTest extends TestCase {

    private static String[] content1 = new String[] {
        "/test",                                                             "nt:unstructured",
        "/test/documents",                                                   "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles",                                          "nt:unstructured",
        "/test/documents/articles/foo",                                      "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/foo/brave-new-world",                      "hippo:handle",
        "/test/documents/articles/foo/brave-new-world/brave-new-world",      "hippo:testdocument",
        "language","english",
        "state","published",
        "/test/documents/articles/foo/brave-new-world/brave-new-world",      "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/foo/brave-new-world2",                     "hippo:handle",
        "/test/documents/articles/foo/brave-new-world2/brave-new-world2",    "hippo:testdocument",
        "language","english",
        "state","published",
        "/test/documents/articles/foo/brave-new-world2/brave-new-world2",    "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/the-invisible-man",                        "hippo:handle",
        "/test/documents/articles/the-invisible-man/the-invisible-man",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document", "hippo:handle",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","dutch"
    };
    private static String[] content2 = new String[] {
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/related", "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/foo",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "select",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/some-image-node", "nt:unstructured",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/some-second-image-node", "nt:unstructured",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "/test/normalselect",                                                "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "select",
        "/test/singledview",                                                  "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "single"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content1);
        session.save();
        build(session, content2);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testFacetedSingledocumentView() throws Exception {
        assertTrue(determineMaxVariants(session.getRootNode().getNode("test/normalselect")) > 1);
        assertTrue(determineMaxVariants(session.getRootNode().getNode("test/singledview")) <= 1);
    }

    private int determineMaxVariants(Node node) throws RepositoryException {
        int count = 0;
        if (node.getPrimaryNodeType().isNodeType(HippoNodeType.NT_HANDLE)) {
            count = (int) node.getNodes(node.getName()).getSize();
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            Node child = iter.nextNode();
            int subcount = determineMaxVariants(child);
            if(subcount > count)
                count = subcount;
        }
        return count;
    }
}
