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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreadcrumbPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BreadcrumbPlugin.class);
    
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
        IModel model = getDefaultModel();
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            List<String> components = new LinkedList<String>();
            while (nodeModel != null) {
                if (nodeModel.getNode() != null) {
                    try {
                        components.add(nodeModel.getNode().getName());
                    } catch (RepositoryException e) {
                        log.error("Error building breadcrumb path", e.getMessage());
                    }
                }
                nodeModel = nodeModel.getParentModel();
            }
            Collections.reverse(components);
            breadcrumb = Strings.join("/", components.toArray(new String[components.size()]));
            redraw();
        }
    }
}
