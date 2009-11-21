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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;

public class NamespaceListingPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    public NamespaceListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected TableDefinition<Node> getTableDefinition() {
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
        });
        columns.add(column);

        return new TableDefinition<Node>(columns);
    }

}
