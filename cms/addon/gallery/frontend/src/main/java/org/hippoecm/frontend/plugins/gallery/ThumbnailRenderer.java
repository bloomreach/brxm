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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ThumbnailRenderer extends AbstractNodeRenderer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    private IPluginContext pluginContext;
    private IPluginConfig pluginConfig;

    public ThumbnailRenderer(IPluginContext pluginContext, IPluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        this.pluginContext = pluginContext;
    }

    @Override
    protected Component getViewer(String id, HippoNode node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (node.hasNode(node.getName())) {
                Node imageSet = node.getNode(node.getName());
                try {
                    Item primItem = imageSet.getPrimaryItem();
                    if (primItem.isNode()) {
                        if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                            return new ImageContainer(id, new JcrNodeModel((Node) primItem), pluginContext, pluginConfig);
                        } else {
                            Gallery.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                        }
                    }
                } catch (ItemNotFoundException e) {
                    Gallery.log.debug("ImageSet must have a primary item. " + node.getPath()
                            + " probably not of correct image set type");
                }
            }
        } else {
            Gallery.log.debug("Node " + node.getPath() + " is not a hippo:handle");
        }
        return new Label(id);
    }
}