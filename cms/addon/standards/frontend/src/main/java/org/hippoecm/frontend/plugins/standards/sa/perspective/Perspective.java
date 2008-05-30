/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.sa.perspective;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.model.ModelService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;

public abstract class Perspective extends RenderPlugin implements ITitleDecorator, IViewService {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";
    public static final String PLUGINS = "perspective.plugins";

    private String title = "title";
    private ModelService modelService;

    public Perspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(TITLE) != null) {
            title = config.getString(TITLE);
        }

        if (config.getString(RenderPlugin.MODEL_ID) != null) {
            modelService = new ModelService(config.getString(RenderPlugin.MODEL_ID), new JcrNodeModel("/"));
            modelService.init(context);
            // unregister self, perspective doesn't need model
            context.registerService(this, RenderPlugin.MODEL_ID);
        }
    }

    // ITitleDecorator

    public String getTitle() {
        return title;
    }

    // IViewService

    public void view(IModel model) {
        modelService.setModel(model);
    }

}
