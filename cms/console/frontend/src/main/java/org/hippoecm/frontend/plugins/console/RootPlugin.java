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
package org.hippoecm.frontend.plugins.console;

import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.hippoecm.frontend.widgets.Pinger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(RootPlugin.class);
    
    private static final long serialVersionUID = 1L;

    private boolean rendered = false;
    
    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.containsKey("pinger.interval")) {
            add(new Pinger("pinger", config.getAsDuration("pinger.interval")));
        } else {
            add(new Pinger("pinger"));
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            String modelId = config.getString(RenderService.MODEL_ID);
            ModelReference modelService = new ModelReference(modelId, new JcrNodeModel("/"));
            modelService.init(context);
            // unregister: don't repaint root plugin when model changes.
            context.unregisterService(this, modelId);
        }

        PageLayoutSettings plSettings = new PageLayoutSettings();
        try {
            PluginConfigMapper.populate(plSettings, config.getPluginConfig("yui.config"));
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
        add(new PageLayoutBehavior(plSettings));
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        super.render(target);
    }

}

