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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;

public class CopyNodeTest extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository server;
    protected Session session;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testVirtualTreeCopy() throws RepositoryException {
        Node node, root = session.getRootNode().addNode("test","hippo:folder");
        node = root.addNode("documents");
        node = node.addNode("document","hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.setProperty("aap", "noot");
        node = root.addNode("navigation");
        node = node.addNode("search",HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[0]);
        session.save();

        assertTrue(root.getNode("navigation").getNode("search").hasNode("documents"));
        assertTrue(root.getNode("navigation").getNode("search").getNode("documents").hasNode("document"));

        ((HippoSession)session).copy(root.getNode("navigation"), "/test/copy");
        session.save();

        assertTrue(root.getNode("copy").getNode("search").hasNode("documents"));
        assertTrue(root.getNode("copy").getNode("search").getNode("documents").hasNode("document"));
    }
}
