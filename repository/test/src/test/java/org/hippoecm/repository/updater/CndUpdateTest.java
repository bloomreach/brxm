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

import java.io.InputStream;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;


public class CndUpdateTest extends RepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        session.getRootNode().addNode("test");
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown(true);
    }

    @Test
    public void testCustomFolderUpdate() throws Exception {
        createNamespace("testUpdateModel", "http://localhost/testUpdateModel/nt/1.0");
        logoutLogin();
        updateModel("testUpdateModel", "cnd1");
        logoutLogin();
        updateModel("testUpdateModel", "cnd2");
        logoutLogin();
        createTestModelContent();
        updateModel("testUpdateModel", "cnd3");
        logoutLogin();
        assertEquals("testUpdateModel:folder", session.getNode("/test/testUpdateModel:folder/testUpdateModel:folder/testUpdateModel:folder[2]").getDefinition().getDeclaringNodeType().getName());
    }

    @Test
    public void testStrictToRelaxedPreservesMultivaluedPropertyType() throws Exception {
        createNamespace("testUpdateModel", "http://localhost/testUpdateModel/nt/1.0");
        logoutLogin();
        updateModel("testUpdateModel", "cnd-not-relaxed");
        logoutLogin();

        final Node document = session.getNode("/test").addNode("document", "testUpdateModel:document");
        document.setProperty("testUpdateModel:multiValuedProp", new String[0]);
        session.save();

        updateModel("testUpdateModel", "cnd-relaxed");
        logoutLogin();

        assertEquals(PropertyType.STRING, session.getProperty("/test/document/testUpdateModel:multiValuedProp").getType());
    }

    @Test
    public void testMoveAggregate() throws Exception {
        createNamespace("testUpdateModel", "http://localhost/testUpdateModel/nt/1.0");
        updateModel("testUpdateModel", "cnd4");
        build(new String[]{
                "/test/doc", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/doc/doc", "testUpdateModel:document",
                "jcr:mixinTypes", "mix:versionable",
                "/test/doc/doc/testUpdateModel:html", "hippostd:html",
                "hippostd:content", "",
                "/test/doc/doc/testUpdateModel:html/link", "hippo:facetselect",
                "hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe",
                "hippo:facets", null,
                "hippo:values", null,
                "hippo:modes", null,
                "/test/doc/doc/testUpdateModel:link", "hippo:mirror",
                "hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe"
        }, session);
        session.save();
        server.close();
        server = HippoRepositoryFactory.getHippoRepository();
        if (background != null) {
            background = server;
        }
        logoutLogin();
        updateModel("testUpdateModel", "cnd5");
        logoutLogin();
    }

    private void createTestModelContent() throws Exception {
        Node folder = createFolder(session.getNode("/test"));
        buildDepth2(folder, 3, 150, 3, 3, 4);
    }

    private void buildDepth2(Node folder, int countDepth2, int countDepth1a, int countDepth1b, int countDepth0a, int countDepth0b) throws Exception {
        for (int i = 0; i <countDepth2; i++) {
            createHandle(folder);
            Node childFolder = createFolder(folder);
            buildDepth1(childFolder, (i == 0 ? countDepth1a : countDepth1b), countDepth0a, countDepth0b);
        }
    }

    private void buildDepth1(Node folder, int countDepth1, int countDepth0a, int countDepth0b) throws Exception {
        for (int i = 0; i < countDepth1; i++) {
            createHandle(folder);
            Node childFolder = createFolder(folder);
            buildDepth0(childFolder, (i == 0 ? countDepth0a : countDepth0b));
            folder.getSession().save();
        }
    }

    private void buildDepth0(Node folder, int countDepth0) throws Exception {
        for (int i = 0; i < countDepth0; i++) {
            createHandle(folder);
            createFolder(folder);
        }
    }

    private void createHandle(final Node folder) throws RepositoryException {
        Node handle = folder.addNode("description", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        handle = handle.addNode("testUpdateModel:description", "testUpdateModel:document");
        handle.addMixin("mix:versionable");
    }

    private Node createFolder(final Node folder) throws RepositoryException {
        Node childFolder = folder.addNode("testUpdateModel:folder", "testUpdateModel:folder");
        childFolder.addMixin("mix:versionable");
        return childFolder;
    }

    private void updateModel(final String prefix, final String cnd) throws Exception {
        final InputStream is = getClass().getResourceAsStream(cnd + ".cnd");
        UpdaterModule updateModelUpdaterModule = new UpdaterModule() {
            public void register(final UpdaterContext context) {
                context.registerName(getClass().getName());
                context.registerStartTag(cnd);
                context.registerEndTag(cnd + "+1");
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, is));
            }
        };
        Modules<UpdaterModule> modules = new Modules<UpdaterModule>(Collections.singletonList(updateModelUpdaterModule));
        UpdaterEngine.migrate(session, modules);
    }

    private void createNamespace(String prefix, String uri) throws Exception {
        WorkflowManager wfmgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        RepositoryWorkflow workflow = (RepositoryWorkflow) wfmgr.getWorkflow("internal", session.getRootNode());
        workflow.createNamespace(prefix, uri);
    }

    private void logoutLogin() throws RepositoryException {
        session.logout();
        session = server.login("admin", "admin".toCharArray());
    }

}
