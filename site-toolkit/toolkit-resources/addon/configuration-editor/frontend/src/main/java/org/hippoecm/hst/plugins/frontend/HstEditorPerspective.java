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

package org.hippoecm.hst.plugins.frontend;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;

public class HstEditorPerspective extends Perspective {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String EDITOR_ROOT = "editor.root";

    public HstEditorPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (!config.containsKey(EDITOR_ROOT)) {
            throw new IllegalArgumentException("Property " + EDITOR_ROOT + " is mandatory");
        }

        JcrNodeModel root = new JcrNodeModel(config.getString(EDITOR_ROOT));
        HstContext hstContext = new HstContext(root);
        context.registerService(hstContext, HstContext.class.getName());

        new ViewController(context, config, root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.editor");
            }
        };

        IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig cluster = pluginConfig.getCluster(config.getString("cluster.name"));
        IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("cluster.options"));

        parameters.put("path", hstContext.config.getPath());
        parameters.put("content.path", hstContext.content.getPath());
        parameters.put("sitemap.path", hstContext.sitemap.getPath());
        context.newCluster(cluster, parameters).start();
    }


}
