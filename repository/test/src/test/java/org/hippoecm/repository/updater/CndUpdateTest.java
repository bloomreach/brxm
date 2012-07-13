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

import java.io.File;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Collections;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Modules;
import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CndUpdateTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private String cnd1 =
            "<testUpdateModel='http://localhost/testUpdateModel/nt/1.0'>\n"
            + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n"
            + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n"
            + "[testUpdateModel:document] > hippo:document\n"
            + "+ testUpdateModel:text\n"
            + "\n"
            + "[testUpdateModel:folder] > hippostd:folder\n";
    private String cnd2 =
            "<testUpdateModel='http://localhost/testUpdateModel/nt/1.1'>\n"
            + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n"
            + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n"
            + "[testUpdateModel:document] > hippo:document\n"
            + "+ testUpdateModel:text\n"
            + "\n"
            + "[testUpdateModel:folder] > hippostd:folder orderable\n"
            + "+ testUpdateModel:description (testUpdateModel:document) multiple\n"
            + "+ testUpdateModel:folder (testUpdateModel:folder) multiple\n";
   private String cnd3 =
            "<testUpdateModel='http://localhost/testUpdateModel/nt/1.2'>\n"
            + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n"
            + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n"
            + "[testUpdateModel:document] > hippo:document\n"
            + "+ testUpdateModel:text\n"
            + "\n"
            + "[testUpdateModel:folder] > hippostd:folder\n"
            + "+ testUpdateModel:description (testUpdateModel:document) multiple\n"
            + "+ testUpdateModel:folder (testUpdateModel:folder) multiple\n";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        session.getRootNode().addNode("test");
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        reloadCnds();
    }

    private RepositoryWorkflow getWorkflow() throws RepositoryException {
        WorkflowManager wfmgr = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        return (RepositoryWorkflow)wfmgr.getWorkflow("internal", session.getRootNode());
    }

    private void flush() throws RepositoryException {
        session.logout();
        session = server.login("admin", "admin".toCharArray());
    }

    private void reloadCnds() {
        File cndChecksums = new File("cnd-checksums");
        if (cndChecksums.exists()) {
            cndChecksums.delete();
        }
    }

    private void buildDepth2(Node n, int countDepth2, int countDepth1a, int countDepth1b, int countDepth0a, int countDepth0b) throws Exception {
        for (int i = 0; i <countDepth2; i++) {
            Node h = n.addNode("description", "hippo:handle");
            h.addMixin("hippo:hardhandle");
            h = h.addNode("testUpdateModel:description", "testUpdateModel:document");
            h.addMixin("hippo:harddocument");
            Node c = n.addNode("testUpdateModel:folder", "testUpdateModel:folder");
            c.addMixin("hippo:harddocument");
            buildDepth1(c, (i==0 ? countDepth1a : countDepth1b), countDepth0a, countDepth0b);
            n.getSession().save();
        }
    }

    private void buildDepth1(Node n, int countDepth1, int countDepth0a, int countDepth0b) throws Exception {
        for (int i = 0; i < countDepth1; i++) {
            Node h = n.addNode("description", "hippo:handle");
            h.addMixin("hippo:hardhandle");
            h = h.addNode("testUpdateModel:description", "testUpdateModel:document");
            h.addMixin("hippo:harddocument");
            Node c = n.addNode("testUpdateModel:folder", "testUpdateModel:folder");
            c.addMixin("hippo:harddocument");
            buildDepth0(c, (i==0 ? countDepth0a : countDepth0b));
        }
    }

    private void buildDepth0(Node n, int countDepth0) throws Exception {
        for (int i = 0; i < countDepth0; i++) {
            Node h = n.addNode("description", "hippo:handle");
            h.addMixin("hippo:hardhandle");
            h = h.addNode("testUpdateModel:description", "testUpdateModel:document");
            h.addMixin("hippo:harddocument");
            Node c = n.addNode("testUpdateModel:folder", "testUpdateModel:folder");
            c.addMixin("hippo:harddocument");
        }
    }

    @Test
    public void testCustomFolderUpdate() throws Exception {
        WorkflowManager wfmgr = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        Workflow wf = wfmgr.getWorkflow("internal", session.getRootNode());
        assertNotNull(wf);
        assertTrue(wf instanceof RepositoryWorkflow);
        RepositoryWorkflow repowf = (RepositoryWorkflow)wf;
        repowf.createNamespace("testUpdateModel", "http://localhost/testUpdateModel/nt/1.0");
        flush();
        updateModel("testUpdateModel", cnd1);
        flush();
        updateModel("testUpdateModel", cnd2);
        flush();
        session.getRootNode().getNode("test").addNode("folder", "testUpdateModel:folder").addMixin("hippo:harddocument");
        session.save();
        buildDepth2(session.getRootNode().getNode("test/folder"), 3, 150,3, 3,4);
        session.save();
        updateModel("testUpdateModel", cnd3);
        flush();
        assertEquals("testUpdateModel:folder", session.getRootNode().getNode("test/folder/testUpdateModel:folder/testUpdateModel:folder[2]").getDefinition().getDeclaringNodeType().getName());
    }

    private String cndMoveAggregate1 =
              "<testUpdateModel='http://localhost/testUpdateModel/nt/1.0'>\n"
            + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n"
            + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n"
            + "[testUpdateModel:document] > hippo:document\n"
            + "+ testUpdateModel:html (hippostd:html)\n"
            + "+ testUpdateModel:link (hippo:mirror)\n";
    private String cndMoveAggregate2 =
              "<testUpdateModel='http://localhost/testUpdateModel/nt/1.1'>\n"
            + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n"
            + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n"
            + "[testUpdateModel:document] > hippo:document\n"
            + "+ testUpdateModel:html (hippostd:html)\n"
            + "+ testUpdateModel:link (hippo:mirror)\n";

    @Test
    public void testMoveAggregate() throws Exception {
       getWorkflow().createNamespace("testUpdateModel", "http://localhost/testUpdateModel/nt/1.0");
       updateModel("testUpdateModel", cndMoveAggregate1);
       build(session, new String[] {
            "/test/doc",                               "hippo:handle",
            "jcr:mixinTypes",                          "hippo:hardhandle",
            "/test/doc/doc",                           "testUpdateModel:document",
            "jcr:mixinTypes",                          "hippo:harddocument",
            "/test/doc/doc/testUpdateModel:html",      "hippostd:html",
            "hippostd:content",                        "",
            "/test/doc/doc/testUpdateModel:html/link", "hippo:facetselect",
            "hippo:docbase",                           "cafebabe-cafe-babe-cafe-babecafebabe",
            "hippo:facets",                            null,
            "hippo:values",                            null,
            "hippo:modes",                             null,
            "/test/doc/doc/testUpdateModel:link",      "hippo:mirror",
            "hippo:docbase",                           "cafebabe-cafe-babe-cafe-babecafebabe"
        });
       session.save();
       flush();
       server.close();
       reloadCnds();
       server = HippoRepositoryFactory.getHippoRepository();
       if (background != null) {
           background = server;
       }
       flush();
       updateModel("testUpdateModel", cndMoveAggregate2);
       flush();
    }


    private void updateModel(final String prefix, final String cnd, final UpdaterModule module) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        UpdaterModule updateModelUpdaterModule = new UpdaterModule() {
            public void register(final UpdaterContext context) {
                context.registerName(module != null ? module.getClass().getName() : getClass().getName());
                context.registerStartTag(null);
                context.registerEndTag(null);
                if (module != null) {
                    module.register(new UpdaterContext() {
                        public void registerName(String name) {
                        }

                        public void registerBefore(String name) {
                        }

                        public void registerAfter(String name) {
                        }

                        public void registerStartTag(String name) {
                        }

                        public void registerExpectTag(String name) {
                        }

                        public void registerEndTag(String name) {
                        }

                        public void registerVisitor(ItemVisitor visitor) {
                            context.registerVisitor(visitor);
                        }

                        public NodeType getNewType(Session session, String type) throws RepositoryException {
                            return context.getNewType(session, type);
                        }

                        public void setName(Item item, String name) throws RepositoryException {
                            context.setName(item, name);
                        }

                        public void setPrimaryNodeType(Node node, String name) throws RepositoryException {
                            context.setPrimaryNodeType(node, name);
                        }

                        public NodeType[] getNodeTypes(Node node) throws RepositoryException {
                            return context.getNodeTypes(node);
                        }

                        public boolean isMultiple(Property property) throws RepositoryException {
                            return context.isMultiple(property);
                        }

                        public Workspace getWorkspace() throws RepositoryException {
                            return context.getWorkspace();
                        }
                    });
                }
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, "-", new StringReader(cnd)));
                //context.registerVisitor(new UpdaterEngine.Cleaner(context));
            }
        };
        Modules<UpdaterModule> modules = new Modules(Collections.singletonList(updateModelUpdaterModule));
        UpdaterEngine.migrate(session, modules);
    }

    public void updateModel(String prefix, String cnd) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
        updateModel(prefix, cnd, null);
    }

}
