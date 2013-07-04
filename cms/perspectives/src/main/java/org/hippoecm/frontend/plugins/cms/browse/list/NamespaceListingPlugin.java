/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
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

public final class NamespaceListingPlugin extends ExpandCollapseListingPlugin<Node> {
    private static final long serialVersionUID = 1L;

    public NamespaceListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName("hippo-typeslist");
        getSettings().setAutoWidthClassName("nslisting-name");
    }

    @Override
    protected TableDefinition<Node> newTableDefinition() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new StringResourceModel("nslisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        column = new ListColumn<Node>(new StringResourceModel("nslisting-type", this, null), null);
        column.setRenderer(new IListCellRenderer<Node>() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, IModel<Node> model) {
                return new Label(id, new StringResourceModel("nslisting-ns", NamespaceListingPlugin.this, null));
            }

            public IObservable getObservable(IModel<Node> model) {
                if (model instanceof IObservable) {
                    return (IObservable) model;
                }
                return null;
            }
        });
        columns.add(column);

        return new TableDefinition<Node>(columns);
    }

    @Override
    protected ISortableDataProvider<Node, String> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

}
