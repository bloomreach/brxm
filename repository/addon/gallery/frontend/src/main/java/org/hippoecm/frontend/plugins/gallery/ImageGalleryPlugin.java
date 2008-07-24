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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractDocumentListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
public class ImageGalleryPlugin extends AbstractDocumentListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ImageGalleryPlugin.class);
    
    public ImageGalleryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<IStyledColumn> getColumns() {
        List<IStyledColumn> columns = new ArrayList<IStyledColumn>();
        columns = new ArrayList<IStyledColumn>();
        columns.add(new ListColumn(new Model("PrimaryItem"), "primaryitem", new PrimaryItemViewer()));
        columns.add(new ListColumn(new Model("Name"), "name", new NameRenderer()));
        return columns;
    }
    
    @Override
    protected Map<String, Comparator> getComparators() {
        Map<String, Comparator> compare;
        compare = new HashMap<String, Comparator>();
        compare.put("name", new NameComparator());
        return compare;
    }

    private static class PrimaryItemViewer extends AbstractNodeRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        protected Component getViewer(String id, HippoNode node) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                if (node.hasNode(node.getName())) {
                    Node imageSet = node.getNode(node.getName());
                    try {
                        Item primItem = imageSet.getPrimaryItem();
                        if (primItem.isNode()) {
                            if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                return new ImageContainer(id, new JcrNodeModel((Node) primItem));
                            } else {
                                ImageGalleryPlugin.log.warn("primary item of image set must be of type "
                                        + HippoNodeType.NT_RESOURCE);
                            }
                        }
                    } catch (ItemNotFoundException e) {
                        ImageGalleryPlugin.log.debug("ImageSet must have a primary item. " + node.getPath()
                                + " probably not of correct image set type");
                    }
                }
            } else {
                ImageGalleryPlugin.log.debug("ImageSet must have a primary item. " + node.getPath()
                        + " probably not of correct image set type");
            }
            return new Label(id);
        }
    }


}
