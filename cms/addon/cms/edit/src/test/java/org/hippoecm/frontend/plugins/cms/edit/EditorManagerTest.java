/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.cms.edit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.junit.Before;
import org.junit.Test;

public class EditorManagerTest extends PluginTest implements IClusterable {

    public static class Editor extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Editor(IPluginContext context, IPluginConfig config) {
            super(context, config);
        }
    }

    String[] content = {
            "/test", "nt:unstructured",
            "/test/content", "nt:unstructured",
            "/test/plugin", "frontend:pluginconfig",
                 "plugin.class", EditorManagerPlugin.class.getName(),
                 "wicket.model", "model",
                 "cluster.edit.name", "editor",
                 "cluster.preview.name", "preview",
                 "/test/plugin/cluster.edit.options", "frontend:pluginconfig",
                     "wicket.id", "editor.renderer",
                 "/test/plugin/cluster.preview.options", "frontend:pluginconfig",
                     "wicket.id", "preview.renderer",
             "/config/test-app/editor", "frontend:plugincluster",
                 "frontend:references", "wicket.model",
                 "frontend:services", "wicket.id",
                 "/config/test-app/editor/plugin", "frontend:plugin",
                     "plugin.class", Editor.class.getName(),
                     "wicket.id", "${wicket.id}",
             "/config/test-app/preview", "frontend:plugincluster",
                 "frontend:references", "wicket.model",
                 "frontend:services", "wicket.id",
                 "/config/test-app/preview/plugin", "frontend:plugin",
                     "plugin.class", Editor.class.getName(),
                     "wicket.id", "${wicket.id}",
    };

    ModelReference modelReference;
    IPluginConfig config;
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);

        modelReference = new ModelReference("model", new JcrNodeModel((Node) null));
        modelReference.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    protected List<IRenderService> getEditors() {
        return context.getServices("editor.renderer", IRenderService.class);
    }

    protected List<IRenderService> getPreviews() {
        return context.getServices("preview.renderer", IRenderService.class);
    }

    @Test
    public void testOpenPreviewOnBrowse() {
        IPluginContext pluginContext = start(config);
        modelReference.setModel(new JcrNodeModel("/test/content"));
        assertEquals(1, getPreviews().size());
    }

}
