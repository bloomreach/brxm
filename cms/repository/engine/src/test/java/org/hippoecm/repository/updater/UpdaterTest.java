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
package org.hippoecm.repository.updater;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.junit.Test;

public class UpdaterTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final String[] content = {
        "/test", "nt:unstructured",
        "/test/docs", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/docs/d", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/d/d", "hippo:document",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/docs/doc", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/doc/doc", "hippo:testdocument",
        "jcr:mixinTypes", "hippo:harddocument",
        "hippo:x", "test"
    };

    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        build(session, content);
        session.save();
    }

    @Test
    public void test() throws RepositoryException {
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
        UpdaterEngine.migrate(session, modules);
    }

    @Test
    public void testReferences() throws RepositoryException, RemoteException, WorkflowException {
        Node container = session.getRootNode().getNode("test");

        // create back references (refA points to earlier node A)
        
        Node A = container.addNode("A", "test:target");
        A.addMixin("hippo:harddocument");

        Node refA = container.addNode("refA", "test:referrer");
        refA.setProperty("referenceA", A);
        Node facetA = refA.addNode("linkA", "hippo:facetselect");
        facetA.setProperty("hippo:docbase", A.getUUID());
        facetA.setProperty("hippo:facets", new String[0]);
        facetA.setProperty("hippo:values", new String[0]);
        facetA.setProperty("hippo:modes", new String[0]);


        // create forward references (refB points to later node B)
        Node refB = container.addNode("refB", "test:referrer");
        Node B = container.addNode("B", "test:target");
        B.addMixin("hippo:harddocument");

        refB.setProperty("referenceB", B);
        Node facetB = refB.addNode("linkB", "hippo:facetselect");
        facetB.setProperty("hippo:docbase", B.getUUID());
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
        UpdaterEngine.migrate(session, modules);


        // verify back references
        
        refA = session.getRootNode().getNode("test/refA");
        A = refA.getProperty("referenceA").getNode();
        assertEquals("test_1_1:target", A.getPrimaryNodeType().getName());

        String uuid = refA.getNode("linkA").getProperty("hippo:docbase").getString();
        assertEquals(A.getUUID(), uuid);


        // verify forward references

        refB = session.getRootNode().getNode("test/refB");
        B = refB.getProperty("referenceB").getNode();
        assertEquals("test_1_1:target", B.getPrimaryNodeType().getName());

        uuid = refB.getNode("linkB").getProperty("hippo:docbase").getString();
        assertEquals(B.getUUID(), uuid);
    }

}
