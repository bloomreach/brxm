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
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.cms.browse.list.resolvers.TemplateTypeIconAttributeModifier;
import org.hippoecm.frontend.plugins.cms.browse.list.resolvers.TemplateTypeRenderer;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypesListingPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TypesListingPlugin.class);

    public TypesListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("typeslisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("typeslisting-type", this, null), null);
        column.setRenderer(new TemplateTypeRenderer());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("typeslisting-state", this, null), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new TemplateTypeIconAttributeModifier());
        columns.add(column);

        return new TableDefinition(columns);
    }

    @Override
    protected ListDataTable getListDataTable(String id, TableDefinition tableDefinition,
            ISortableDataProvider dataProvider, TableSelectionListener selectionListener, final boolean triState,
            ListPagingDefinition pagingDefinition) {
        return new ListDataTable(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IObserver newObserver(final Item item, IModel model) {
                if (model instanceof JcrNodeModel) {
                    Node node = ((JcrNodeModel) model).getNode();
                    try {
                        if (node != null && node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                            final JcrNodeModel nodeModel = new JcrNodeModel(node
                                    .getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE));
                            return new IObserver() {
                                private static final long serialVersionUID = 7399282716376782006L;

                                public IObservable getObservable() {
                                    return nodeModel;
                                }

                                public void onEvent(Iterator<? extends IEvent> events) {
                                    redrawItem(item);
                                }

                            };
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
                return super.newObserver(item, model);
            }
        };
    }

}
