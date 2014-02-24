/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.TagTester;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkflowManagerPluginTest extends PluginTest {

    public interface TestWorkflow extends Workflow {

        void doit();
    }

    public static class TestWorkflowImpl extends WorkflowImpl implements TestWorkflow {

        public TestWorkflowImpl() throws RemoteException {
        }

        public void doit() {
        }
    }

    public static class TestWorkflowPlugin extends RenderPlugin {

        public TestWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
            super(context, config);

            context.registerService(this, "workflow.plugin");

            add(new StdWorkflow<Workflow>("doit", "DO IT") {

            });
        }

        public String getCategory() {
            return getPluginConfig().getString("category");
        }
    }

    static String[] category = {
            "/${name}", "hipposys:workflowcategory",
                "/${name}/test", "frontend:workflow",
                    "hipposys:classname", TestWorkflowImpl.class.getName(),
                    "hipposys:nodetype", "hippo:handle",
                    "hipposys:subtype", "frontendtest:document",
                    "/${name}/test/frontend:renderer", "frontend:plugin",
                        "plugin.class", TestWorkflowPlugin.class.getName(),
                        "category", "${name}"
    };

    static String[] content = {
            "/folder", "hippostd:folder",
                "jcr:mixinTypes", "mix:referenceable",
                "/folder/doc", "hippo:handle",
                    "jcr:mixinTypes", "mix:referenceable",
                    "/folder/doc/doc", "frontendtest:document",
                        "jcr:mixinTypes", "mix:versionable"
    };

    @Before
    public void doBootstrap() throws RepositoryException {
        {
            Map<String, String> params = new HashMap<>();
            params.put("name", "plugin-test");
            build(session, mount("/hippo:configuration/hippo:workflows", instantiate(category, params)));
        }

        {
            Map<String, String> params = new HashMap<>();
            params.put("name", "versioning-test");
            build(session, mount("/hippo:configuration/hippo:workflows", instantiate(category, params)));
        }

        session.getRootNode().addNode("test");
        build(session, mount("/test", content));
        session.save();
    }

    @After
    public void cleanupBootstrap() throws RepositoryException {
        session.getNode("/hippo:configuration/hippo:workflows/plugin-test").remove();
        session.getNode("/hippo:configuration/hippo:workflows/versioning-test").remove();
        session.save();
    }

    @Test
    public void workflowCategoryIsUsedAsMenu() {
        final JavaPluginConfig config = new JavaPluginConfig();
        config.put("wicket.id", "service.root");
        config.put("workflow.categories", new String[]{"plugin-test"});

        TestWorkflowManagerPlugin manager = new TestWorkflowManagerPlugin(context, config);
        manager.setModel(new JcrNodeModel("/test/folder/doc"));
        tester.startPage(home);

        TestWorkflowPlugin plugin = context.getService("workflow.plugin", TestWorkflowPlugin.class);
        assertEquals("plugin-test", plugin.getCategory());

        Component button = tester.getComponentFromLastRenderedPage("root:menu:list:0:item:link");
        final TagTester tagTester = tester.getTagById(button.getMarkupId());
        assertTrue(tagTester.getValue().contains("plugin-test"));
    }

    @Test
    public void documentWorkflowManagerUsesHandleDocumentWorkflowForVersioning() throws RepositoryException {
        final JavaPluginConfig config = new JavaPluginConfig();
        config.put("wicket.id", "service.root");
        config.put("workflow.categories", new String[]{"plugin-test"});
        config.put("workflow.version.categories", new String[]{"versioning-test"});

        final Node docNode = session.getNode("/test/folder/doc/doc");
        final Version checkin = docNode.checkin();

        DocumentWorkflowManagerPlugin manager = new DocumentWorkflowManagerPlugin(context, config);
        manager.setModel(new JcrNodeModel(checkin.getFrozenNode()));
        tester.startPage(home);

        TestWorkflowPlugin plugin = context.getService("workflow.plugin", TestWorkflowPlugin.class);
        assertEquals("versioning-test", plugin.getCategory());

        WorkflowDescriptorModel model = (WorkflowDescriptorModel) plugin.getModel();
        WorkflowDescriptor descriptor = model.getObject();
        Workflow workflow = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow(descriptor);
        assertTrue(workflow instanceof TestWorkflow);
    }
}
