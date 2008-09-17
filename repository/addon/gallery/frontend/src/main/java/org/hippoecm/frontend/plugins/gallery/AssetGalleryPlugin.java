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
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class AssetGalleryPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    public AssetGalleryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model("Type"), null);
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        columns.add(column);

        column = new ListColumn(new Model("Name"), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        return new TableDefinition(columns);
    }

    @Override
    protected ISortableDataProvider getDataProvider() {
        return new DocumentsProvider((JcrNodeModel) getModel(), getTableDefinition().getComparators());
    }

    private class MimeTypeAttributeModifier extends AbstractNodeAttributeModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
            String cssClass;
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node imageSet = node.getNode(node.getName());
                try {
                    Item primItem = imageSet.getPrimaryItem();
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        String mimeType = ((Node) primItem).getProperty("jcr:mimeType").getString();
                        if (mimeType.startsWith("application")) {
                            cssClass = mimeType;
                            cssClass = StringUtils.replace(cssClass, "/", "-");
                            cssClass = StringUtils.replace(cssClass, ".", "-");
                        } else {
                            cssClass = StringUtils.substringBefore(mimeType, "/");    
                        }
                        cssClass = "mimetype-" + cssClass + "-16";
                    } else {
                        Gallery.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                        return null;
                    }
                } catch (ItemNotFoundException e) {
                    Gallery.log.warn("ImageSet must have a primary item. " + node.getPath()
                            + " probably not of correct image set type");
                    return null;
                }
            } else {
                Gallery.log.warn("Node " + node.getPath() + " is not a hippo:handle");
                return null;
            }
            return new CssClassAppender(new Model(cssClass));
        }

        @Override
        public AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
            return new CssClassAppender(new Model("icon-16"));
        }
    }
}
