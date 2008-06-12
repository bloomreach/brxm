/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.decorating;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoNodeType;

import org.hippoecm.repository.TestCase;
import org.junit.*;
import static org.junit.Assert.*;

public class SingledViewFacetSelectTest  extends TestCase {
    private static String[] contents = new String[] {
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
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","dutch",
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

    public void setUp() throws Exception {
        super.setUp();
        build(session, contents);
        session.save();
    }

    @Test public void testFacetedSingledocumentView() throws Exception {
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node normalSelectNode = session.getRootNode().getNode("test/normalselect");
        Node singledViewNode = session.getRootNode().getNode("test/singledview");
        assertTrue(confirmMultiple(normalSelectNode));
        assertTrue(confirmSingle(singledViewNode));
    }
    
    private boolean confirmSingle(Node singledViewNode) throws RepositoryException {
        SingledView singledView = new SingledView();
        traverse(singledViewNode,singledView);
        return singledView.isSingleView();
    }

    private boolean confirmMultiple(Node normalSelectNode) throws RepositoryException {
        SingledView singledView = new SingledView();
        traverse(normalSelectNode,singledView);
        return !singledView.isSingleView();
        
    }

    protected void traverse(Node node, SingledView singledView) throws RepositoryException {
        if(node.getPrimaryNodeType().isNodeType(HippoNodeType.NT_HANDLE) && node.getNodes().getSize() > 1) {
            singledView.setSingleView(false);
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            traverse(child, singledView);
        }
    }

    class SingledView {
        boolean singleView = true;

        public boolean isSingleView() {
            return singleView;
        }

        public void setSingleView(boolean singleView) {
            this.singleView = singleView;
        }
    }
}
