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
package org.hippoecm.frontend.plugins.cms.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;

public class ToggleVersionPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public ToggleVersionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        add(new Label("toggleLabel"));
        if(getModel() != null) {
            onModelChanged();
        }
    }
    
    @Override
    protected void onModelChanged() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        try {
            Node node = model.getNode().getCanonicalNode();
            if(node == null) {
                // virtual node not having canonical equivalent
                return;
            }
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                replace(new ToggleVersionPanel("toggleLabel"));
            } else {
                replace(new Label("toggleLabel"));
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        redraw();
    }
    
}
