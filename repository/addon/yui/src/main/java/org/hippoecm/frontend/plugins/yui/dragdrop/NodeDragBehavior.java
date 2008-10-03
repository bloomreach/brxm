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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDragBehavior extends DragBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeDragBehavior.class);

    final String nodePath;//TODO: remove this
    final JcrNodeModel nodeModel;
    
    public NodeDragBehavior(IPluginContext context, IPluginConfig config, String nodePath) {
        super(context, config);
        this.nodePath = nodePath;
        this.nodeModel = new JcrNodeModel(nodePath);
    }

    @Override
    protected IModel getDragModel() {
        return nodeModel;
    }
    
    @Override
    protected String getLabel() {
        try {
            return nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            log.error("Failed to retrieve displayname", e);
        }
        return nodeModel.getItemModel().getPath();
    }
    
    @Override
    public void detach(Component component) {
        super.detach(component);
        this.nodeModel.detach();
    }
}
