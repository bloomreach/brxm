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
package org.hippoecm.frontend.plugins.console.breadcrumb;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.ISO9075Helper;

public class BreadcrumbPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private String breadcrumb;

    public BreadcrumbPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(new Label("breadcrumb", new PropertyModel(this, "breadcrumb")));
        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        IModel model = getModel();
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeNodel = (JcrNodeModel)model;
            breadcrumb = ISO9075Helper.decodeLocalName(nodeNodel.getItemModel().getPath());
        }
    }
}
