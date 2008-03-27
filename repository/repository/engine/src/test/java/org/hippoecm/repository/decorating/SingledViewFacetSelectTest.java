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

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoNodeType;

public class SingledViewFacetSelectTest  extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository repository;
    protected Session session;
    
    private static String[] contents = new String[] {
        "/documents",                                                   "nt:unstructured",
        "/documents/articles",                                          "nt:unstructured",
        "/documents/articles/foo",                                          "hippo:folder",
        "/documents/articles/foo/brave-new-world",                          "hippo:handle",
        "/documents/articles/foo/brave-new-world/brave-new-world",          "hippo:testdocument",
        "language","english",
        "state","published",
        "/documents/articles/foo/brave-new-world/brave-new-world",          "hippo:testdocument",
        "language","dutch",
        "/documents/articles/foo/brave-new-world2",                          "hippo:handle",
        "/documents/articles/foo/brave-new-world2/brave-new-world2",          "hippo:testdocument",
        "language","english",
        "state","published",
        "/documents/articles/foo/brave-new-world2/brave-new-world2",          "hippo:testdocument",
        "language","dutch",
        "/documents/articles/the-invisible-man",                        "hippo:handle",
        "/documents/articles/the-invisible-man/the-invisible-man",      "hippo:testdocument",
        "language","english",
        "/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","english",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document", "hippo:handle",
        "language","english",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","english",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","dutch",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/related", "hippo:facetselect",
        "hippo:docbase", "/documents/articles/foo",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "select",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/some-image-node", "nt:unstructured",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds/some-second-image-node", "nt:unstructured",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","dutch",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "/normalselect",                                                "hippo:facetselect",
        "hippo:docbase", "/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "select",
        "/singledview",                                                  "hippo:facetselect",
        "hippo:docbase", "/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "single"
    };

    
    
    public void testFacetedSingledocumentView() throws Exception {
        Session session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node normalSelectNode = session.getRootNode().getNode("normalselect");
        Node singledViewNode = session.getRootNode().getNode("singledview");
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
            traverse(child,singledView);
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
    
    public void setUp() throws Exception {
        repository = null;
        repository = HippoRepositoryFactory.getHippoRepository();
        session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        FacetContentUtilities.build(session, contents);
        session.save();
        session.logout();
    }
    
    public void tearDown() throws Exception {
        session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node = session.getRootNode();
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system")) {
                child.remove();
            }
        }
        session.save();
        if(session != null) {
            session.logout();
        }
        if (repository != null) {
            repository.close();
            repository = null;
        }
    }
}
