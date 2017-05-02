/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.render;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.ImageContainer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbnailRenderer extends AbstractNodeRenderer {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailRenderer.class);

    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    public ThumbnailRenderer(final IPluginContext pluginContext, final IPluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        this.pluginContext = pluginContext;
    }

    @Override
    protected Component getViewer(final String id, final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (node.hasNode(node.getName())) {
                final Node imageSet = node.getNode(node.getName());
                try {
                    final Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode()) {
                        if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                            return new ImageContainer(id, new JcrNodeModel((Node) primItem), pluginContext,
                                    pluginConfig);
                        } else {
                            log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                        }
                    }
                } catch (final ItemNotFoundException e) {
                    log.debug("ImageSet must have a primary item. " + node.getPath()
                            + " probably not of correct image set type");
                }
            }
        } else {
            log.debug("Node " + node.getPath() + " is not a hippo:handle");
        }
        return new Label(id);
    }
}
