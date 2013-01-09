/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentTypeIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

public final class DefaultListColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;

    public DefaultListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //Type Icon
        ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), "icon");
        column.setComparator(new TypeComparator());
        String iconRenderer = getPluginConfig().getString("documentTypeIconRenderer");
        if ("cssIconRenderer".equals(iconRenderer)) {
            column.setRenderer(new EmptyRenderer<Node>());
            column.setAttributeModifier(new DocumentTypeIconAttributeModifier());
        } else if ("resourceIconRenderer".equals(iconRenderer)) {
            column.setRenderer(new IconRenderer());
            column.setAttributeModifier(new IconAttributeModifier());
        } else {
            column.setRenderer(new IconRenderer());
            column.setAttributeModifier(new IconAttributeModifier());
        }
        column.setCssClass("doclisting-icon");
        columns.add(column);

        //Name
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-name", DocumentListingPlugin.class), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass("doclisting-name");
        columns.add(column);

        return columns;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();

        //Type
        ListColumn<Node> column = new ListColumn<Node>(
                new ClassResourceModel("doclisting-type", DocumentListingPlugin.class), "type");
        column.setComparator(new TypeComparator());
        column.setRenderer(new TypeRenderer());
        column.setCssClass("doclisting-type");
        columns.add(column);

        return columns;
    }

}
