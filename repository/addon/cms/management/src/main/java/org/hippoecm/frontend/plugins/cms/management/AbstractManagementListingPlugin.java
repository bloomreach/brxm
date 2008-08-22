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
package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagementListingPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(AbstractManagementListingPlugin.class);

    public AbstractManagementListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();
        columns.add(new ListColumn(new Model("Name"), "name"));
        return new TableDefinition(columns, false);
    }

    @Override
    protected ISortableDataProvider getDataProvider() {
        return new SortableDataProvider() {
            private static final long serialVersionUID = 1L;

            public Iterator iterator(int first, int count) {
                return getRows().subList(first, first + count).iterator();
            }

            public IModel model(Object object) {
                return (JcrNodeModel) object;
            }

            public int size() {
                return getRows().size();
            }

            public void detach() {
            }
        };
    }

    protected abstract List<IModel> getRows();

}
