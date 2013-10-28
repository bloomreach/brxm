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
package org.hippoecm.repository.updater;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdaterTest extends RepositoryTestCase {

    private final String[] content = {
        "/test", "nt:unstructured",
        "/test/docs", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/docs/d", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/d/d", "hippo:document",
        "jcr:mixinTypes", "mix:versionable",
        "/test/docs/doc", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/doc/doc", "hippo:testdocument",
        "jcr:mixinTypes", "mix:versionable",
        "hippo:x", "test"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);
        session.save();
    }

    @Test
    public void testReorder() throws RepositoryException {
        Node testNode = session.getNode("/test");
        testNode.addNode("a");
        testNode.addNode("b");
        session.save();

        UpdaterSession us = new UpdaterSession(session);
        Node updaterTestNode = us.getRootNode().getNode("test");
        updaterTestNode.orderBefore("b", "a");

        us.commit();

        List<String> names = new LinkedList<String>();
        for (javax.jcr.NodeIterator ni = testNode.getNodes(); ni.hasNext(); ) {
            Node node = ni.nextNode();
            names.add(node.getName());
        }
        assertTrue("nodes have not been reordered", names.indexOf("b") < names.indexOf("a"));

        updaterTestNode = us.getRootNode().getNode("test");
        updaterTestNode.orderBefore("b", null);
        us.commit();

        names.clear();
        for (javax.jcr.NodeIterator ni = testNode.getNodes(); ni.hasNext(); ) {
            Node node = ni.nextNode();
            names.add(node.getName());
        }
        assertEquals("node was not moved to end", names.size() - 1, names.indexOf("b"));
    }

    @Test
    public void testMigrate() throws RepositoryException {
        UpdaterModule module = new UpdaterModule() {
            public void register(UpdaterContext context) {
                context.registerVisitor(new UpdaterItemVisitor.Default() {
                    @Override
                    public void leaving(Node visit, int level) throws RepositoryException {
                        if(visit.hasProperty("hippo:x")) {
                            visit.getProperty("hippo:x").remove();
                        }
                        if (visit.getPath().equals("/test/docs/d/d")) {
                            ((UpdaterNode) visit).setPrimaryNodeType("hippo:testdocument");
                            visit.setProperty("hippo:y", "bla");
                        }
                    }
                });
            }
        };
        List list = new LinkedList();
        list.add(module);
        Modules modules = new Modules(list);
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        UpdaterEngine.migrate(session, modules);
        session.logout();
    }

    @Test
    public void testReferences() throws RepositoryException, RemoteException, WorkflowException {
        Node container = session.getRootNode().getNode("test");

        // create back references (refA points to earlier node A)
        
        Node A = container.addNode("A", "test:target");
        A.addMixin("mix:versionable");

        Node refA = container.addNode("refA", "test:referrer");
        refA.setProperty("referenceA", A);
        Node facetA = refA.addNode("linkA", "hippo:facetselect");
        facetA.setProperty("hippo:docbase", A.getIdentifier());
        facetA.setProperty("hippo:facets", new String[0]);
        facetA.setProperty("hippo:values", new String[0]);
        facetA.setProperty("hippo:modes", new String[0]);


        // create forward references (refB points to later node B)
        Node refB = container.addNode("refB", "test:referrer");
        Node B = container.addNode("B", "test:target");
        B.addMixin("mix:versionable");

        refB.setProperty("referenceB", B);
        Node facetB = refB.addNode("linkB", "hippo:facetselect");
        facetB.setProperty("hippo:docbase", B.getIdentifier());
        facetB.setProperty("hippo:facets", new String[0]);
        facetB.setProperty("hippo:values", new String[0]);
        facetB.setProperty("hippo:modes", new String[0]);

        session.save();

        UpdaterModule module = new UpdaterModule() {
            String newTest  = 
                "<test='http://www.onehippo.org/jcr/hippo/test/nt/1.1'>"+
                "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"+
                "[test:target] > hippo:document "+
                "[test:referrer] > hippo:document - * (reference) + * (hippo:facetselect)";

            public void register(UpdaterContext context) {
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "test", "-", new StringReader(newTest)));
            }
        };
        List list = new LinkedList();
        list.add(module);
        Modules modules = new Modules(list);
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        UpdaterEngine.migrate(session, modules);
        session.logout();

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        // verify back references
        
        refA = session.getRootNode().getNode("test/refA");
        A = refA.getProperty("referenceA").getNode();
        assertEquals("test:target", A.getPrimaryNodeType().getName());

        String uuid = refA.getNode("linkA").getProperty("hippo:docbase").getString();
        assertEquals(A.getIdentifier(), uuid);

        // verify forward references

        refB = session.getRootNode().getNode("test/refB");
        B = refB.getProperty("referenceB").getNode();
        assertEquals("test:target", B.getPrimaryNodeType().getName());

        uuid = refB.getNode("linkB").getProperty("hippo:docbase").getString();
        assertEquals(B.getIdentifier(), uuid);
    }
}
