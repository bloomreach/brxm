/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAndStateRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;
import org.hippoecm.frontend.skin.DocumentListColumn;

public final class DefaultListColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;

    public DefaultListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        final List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();
        columns.add(createIconAndStateColumn());
        columns.add(createNameColumn());
        return columns;
    }

    private ListColumn<Node> createIconAndStateColumn() {
        final ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new IconAndStateRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

    private ListColumn<Node> createNameColumn() {
        final ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("doclisting-name", DocumentListingPlugin.class), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        return column;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        final List<ListColumn<Node>> columns = getColumns();
        columns.add(createTypeColumn());
        return columns;
    }

    private ListColumn<Node> createTypeColumn() {
        ListColumn<Node> column = new ListColumn<Node>(
                new ClassResourceModel("doclisting-type", DocumentListingPlugin.class), "type");
        column.setComparator(new TypeComparator());
        column.setRenderer(new TypeRenderer());
        column.setCssClass(DocumentListColumn.TYPE.getCssClass());
        return column;
    }

}
