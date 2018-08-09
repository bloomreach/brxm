/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.layout;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.builder.ExtensionPointLocator;
import org.hippoecm.frontend.editor.builder.ILayoutAware;
import org.hippoecm.frontend.editor.builder.PreviewClusterConfig;
import org.hippoecm.frontend.editor.builder.RenderPluginEditorPlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TwoColumnTest extends PluginTest {

    String[] content = {
            "/test", "nt:unstructured",
                "/test/plugin", "frontend:plugin",
                    "plugin.class", RenderPluginEditorPlugin.class.getName(),
                "/test/config", "frontend:plugincluster",
                    "frontend:services", "wicket.id",
                    "/test/config/layout", "frontend:plugin",
                        "plugin.class", getClass().getPackage().getName() + ".TwoColumn",
                        // "wicket.extensions", "extension.left",
                        // "wicket.extensions", "extension.right",
                        "extension.left", "${cluster.id}.left",
                        "extension.right", "${cluster.id}.right",
                    "/test/config/left", "frontend:plugin",
                        "plugin.class", ListViewPlugin.class.getName(),
                        "wicket.id", "${cluster.id}.left",
                        "item", "${cluster.id}.left.item",
                    "/test/config/right", "frontend:plugin",
                        "plugin.class", ListViewPlugin.class.getName(),
                        "wicket.id", "${cluster.id}.right",
                        "item", "${cluster.id}.right.item"
    };

    private IClusterConfig editedCluster;
    private IClusterControl builderControl;
    private String selectedPluginId;
    private String clusterModelId;
    private ExtensionPointLocator locator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        build(session, content);
        root.getNode("test/config/layout").setProperty("wicket.extensions",
                new String[] { "extension.left", "extension.right" });
        editedCluster = new JcrClusterConfig(new JcrNodeModel("/test/config"));

        clusterModelId = "service.model.cluster";
        ModelReference clusterModelService = new ModelReference(clusterModelId, new Model());
        clusterModelService.init(context);

        selectedPluginId = "service.model.selected_plugin";
        ModelReference selectedPluginService = new ModelReference(selectedPluginId, null);
        selectedPluginService.init(context);

        locator = new ExtensionPointLocator(selectedPluginService);

        context.getService(clusterModelId, IModelReference.class).setModel(new Model(editedCluster));

        Map<String, String> builderPluginParameters = new TreeMap<String, String>();
        builderPluginParameters.put("wicket.helper.id", "service.helpers");
        builderPluginParameters.put("wicket.model", clusterModelId);
        builderPluginParameters.put("model.plugin", selectedPluginId);
        PreviewClusterConfig template = new PreviewClusterConfig(editedCluster, builderPluginParameters, true);

        IPluginConfig parameters = new JavaPluginConfig();
        parameters.put("wicket.id", "service.root");
        builderControl = context.newCluster(template, parameters);
        builderControl.start();

        locator.setLayoutAwareRoot(context.getService("service.root", ILayoutAware.class));

        // render page
        refreshPage();
    }
    
    protected void restartCluster() {
        // restart cluster
        builderControl.stop();

        Map<String, String> builderPluginParameters = new TreeMap<String, String>();
        builderPluginParameters.put("wicket.helper.id", "service.helpers");
        builderPluginParameters.put("wicket.model", clusterModelId);
        builderPluginParameters.put("model.plugin", selectedPluginId);
        PreviewClusterConfig template = new PreviewClusterConfig(editedCluster, builderPluginParameters, true);

        IPluginConfig parameters = new JavaPluginConfig();
        parameters.put("wicket.id", "service.root");

        builderControl = context.newCluster(template, parameters);
        builderControl.start();

        locator.setLayoutAwareRoot(context.getService("service.root", ILayoutAware.class));

        refreshPage();
    }
    
    @Test
    public void testListExtensionPointSelected() throws Exception {
        // select left list
        tester.executeAjaxEvent("root:preview:extension.left", "click");
        assertEquals("${cluster.id}.left.item", locator.getSelectedExtensionPoint());

        // select right list
        tester.executeAjaxEvent("root:preview:extension.right", "click");
        assertEquals("${cluster.id}.right.item", locator.getSelectedExtensionPoint());

//        Utilities.dump(root.getNode("test"));
//        printComponents(System.out);
    }

    @Test
    public void testMovePlugin() throws Exception {
        List<IPluginConfig> plugins = editedCluster.getPlugins();
        LinkedList<IPluginConfig> newPlugins = new LinkedList<IPluginConfig>(plugins);
        JavaPluginConfig newConfig = new JavaPluginConfig("test");
        newConfig.put("plugin.class", RenderPlugin.class.getName());
        newConfig.put("wicket.id", "${cluster.id}.left.item");
        newPlugins.add(newConfig);
        editedCluster.setPlugins(newPlugins);

        restartCluster();

        // verify that plugin is there
        tester.assertComponent("root:preview:extension.left:preview:view:1:item:preview", RenderPlugin.class);

        // move to right
        tester.clickLink("root:preview:extension.left:preview:view:1:item:head:transitions:1:link");
        
        restartCluster();

        // plugin should now be at the right
        tester.assertComponent("root:preview:extension.right:preview:view:1:item:preview", RenderPlugin.class);

//        Utilities.dump(root.getNode("test"));
//        printComponents(System.out);
    }

}
