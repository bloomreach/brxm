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
package org.hippoecm.frontend.editor.workflow;

import javax.jcr.Node;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.layout.LayoutProviderPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NamespaceWorkflowPluginTest extends PluginTest {

    public static final class MenuTesterPlugin extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public MenuTesterPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerTracker(new ServiceTracker<IRenderService>(IRenderService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(IRenderService service, String name) {
                    addOrReplace(new MenuTester("menu", (MarkupContainer) service.getComponent()));
                    redraw();
                }

            }, config.getString("actions"));
        }
    }

    final static String[] content = {
        "/test", "nt:unstructured",
            "/test/menu", "frontend:plugin",
                "plugin.class", MenuTesterPlugin.class.getName(),
                "wicket.id", "service.root",
                "actions", "service.actions",
            "/test/layouts", "frontend:plugin",
                "plugin.class", LayoutProviderPlugin.class.getName(),
            "/test/plugin", "frontend:plugin",
                "plugin.class", NamespaceWorkflowPlugin.class.getName(),
                "wicket.id", "service.actions",
                "wicket.model", "service.model",
    };

    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        //TODO: clear repository
        super.setUp();
        build(session, content);

        start(new JcrPluginConfig(new JcrNodeModel("/test/layouts")));
        start(new JcrPluginConfig(new JcrNodeModel("/test/menu")));

        WorkflowManager wflMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        if (!session.itemExists("/hippo:namespaces/testns")) {
            Node nsNode = session.getRootNode().getNode("hippo:namespaces");
            TemplateEditorWorkflow nsWfl = (TemplateEditorWorkflow) wflMgr.getWorkflow("test", nsNode);
            nsWfl.createNamespace("testns", "http://example.org/test/0.0");
        }

        Node documentNode = session.getRootNode().getNode("hippo:namespaces/testns");
        String category = "test";
        WorkflowDescriptorModel pluginModel = new WorkflowDescriptorModel(category, documentNode);

        ModelReference ref = new ModelReference("service.model", pluginModel);
        ref.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void createDocumentTypeTest() {
        start(config);
        refreshPage();

        // "new document type"
        tester.clickLink("root:menu:1:link");
        //        printComponents(System.out);

        // "test-type"
        FormTester formTest = tester.newFormTester("dialog:content:form");
        formTest.setValue("view:name:widget", "testtype");

        // "next"
        tester.executeAjaxEvent("dialog:content:form:buttons:next", "onclick");

        // "select layout"
        tester.clickLink("dialog:content:form:view:layouts:0:link");

        tester.executeAjaxEvent("dialog:content:form:buttons:finish", "onclick");

        JcrNodeModel nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype");
        assertTrue(nsNode.getItemModel().exists());

        nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype/editor:templates/_default_");
        assertTrue(nsNode.getItemModel().exists());
    }

    @Test
    public void createCompoundTypeTest() {
        start(config);
        refreshPage();

        // "new document type"
        tester.clickLink("root:menu:2:link");
//        printComponents(System.out);

        // "test-type"
        FormTester formTest = tester.newFormTester("dialog:content:form");
        formTest.setValue("view:name:widget", "testtype");

        // "next"
        tester.executeAjaxEvent("dialog:content:form:buttons:next", "onclick");

        // "select layout"
        tester.clickLink("dialog:content:form:view:layouts:0:link");
        tester.executeAjaxEvent("dialog:content:form:buttons:finish", "onclick");

        JcrNodeModel nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype");
        assertTrue(nsNode.getItemModel().exists());

        nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype/editor:templates/_default_");
        assertTrue(nsNode.getItemModel().exists());
    }

}
