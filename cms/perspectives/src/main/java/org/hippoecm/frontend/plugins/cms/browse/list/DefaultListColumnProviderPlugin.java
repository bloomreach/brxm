/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentIconAndStateRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.skin.DocumentListColumn;

public final class DefaultListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private final ListColumn<Node> ICON_AND_STATE_COLUMN = createIconAndStateColumn();
    private final ListColumn<Node> NAME_COLUMN = DocumentListingPlugin.createNameColumn();
    private final ListColumn<Node> TYPE_COLUMN = DocumentListingPlugin.createTypeColumn();

    private final List<ListColumn<Node>> COLLAPSED_COLUMNS = Arrays.asList(ICON_AND_STATE_COLUMN, NAME_COLUMN);
    private final List<ListColumn<Node>> TYPE_VIEW_COLUMNS = Arrays.asList(ICON_AND_STATE_COLUMN, NAME_COLUMN, TYPE_COLUMN);
    private final List<ListColumn<Node>> EXPANDED_COLUMNS = Arrays.asList(ICON_AND_STATE_COLUMN, NAME_COLUMN, TYPE_COLUMN);

    public DefaultListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        return COLLAPSED_COLUMNS;
    }

    @Override
    public List<ListColumn<Node>> getTypeViewColumns() {
        return TYPE_VIEW_COLUMNS;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        return EXPANDED_COLUMNS;
    }

    private static ListColumn<Node> createIconAndStateColumn() {
        final Model<String> iconHeader = Model.of(StringUtils.EMPTY);
        final ListColumn<Node> column = new ListColumn<>(iconHeader, "icon");
        column.setComparator(StateComparator.getInstance());
        column.setRenderer(new DocumentIconAndStateRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

}
