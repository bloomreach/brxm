/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.SpriteIconRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.frontend.skin.Icon;

public final class NamespaceListingPlugin extends ExpandCollapseListingPlugin<Node> {
    private static final long serialVersionUID = 1L;

    public NamespaceListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS);
    }

    @Override
    protected TableDefinition<Node> newTableDefinition() {
        List<ListColumn<Node>> columns = new ArrayList<>();
        columns.add(createIconColumn());
        columns.add(createNameColumn());
        columns.add(createTypeColumn());
        return new TableDefinition<>(columns);
    }

    private ListColumn<Node> createIconColumn() {
        final ListColumn<Node> column = new ListColumn<>(Model.of(StringUtils.EMPTY), null);
        column.setRenderer(new SpriteIconRenderer(Icon.FOLDER, IconSize.L));
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

    private ListColumn<Node> createNameColumn() {
        final StringResourceModel nameHeader = new StringResourceModel("nslisting-name", this);
        ListColumn<Node> column = new ListColumn<>(nameHeader, "name");
        column.setComparator(NameComparator.getInstance());
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        return column;
    }

    private ListColumn<Node> createTypeColumn() {
        final StringResourceModel typeHeader = new StringResourceModel("nslisting-type", this);
        final ListColumn<Node> column = new ListColumn<>(typeHeader, null);
        column.setRenderer(new NameSpaceRenderer());
        column.setCssClass(DocumentListColumn.TYPE.getCssClass());
        return column;
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    private class NameSpaceRenderer implements IListCellRenderer<Node> {
        private static final long serialVersionUID = 1L;

        public Component getRenderer(String id, IModel<Node> model) {
            final StringResourceModel namespace = new StringResourceModel("nslisting-ns", NamespaceListingPlugin.this);
            return new Label(id, namespace);
        }

        public IObservable getObservable(IModel<Node> model) {
            if (model instanceof IObservable) {
                return (IObservable) model;
            }
            return null;
        }
    }
}
