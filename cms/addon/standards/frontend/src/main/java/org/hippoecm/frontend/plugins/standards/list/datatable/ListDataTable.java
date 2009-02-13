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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.widgets.ManagedReuseStrategy;

public class ListDataTable extends DataTable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private Map<Item, IObserver> observers;
    private Set<Item> dirty;
    private TableSelectionListener selectionListener;
    private IDataProvider provider;

    public interface TableSelectionListener {
        public void selectionChanged(IModel model);
    }

    public ListDataTable(String id, TableDefinition tableDefinition, ISortableDataProvider dataProvider,
            TableSelectionListener selectionListener, final boolean triState, ListPagingDefinition pagingDefinition) {
        super(id, tableDefinition.getColumns(), dataProvider, pagingDefinition.getPageSize());
        setOutputMarkupId(true);
        setVersioned(false);

        this.provider = dataProvider;
        this.selectionListener = selectionListener;

        if (tableDefinition.showColumnHeaders()) {
            addTopToolbar(new AjaxFallbackHeadersToolbar(this, dataProvider) {
                private static final long serialVersionUID = 1L;

                @Override
                protected WebMarkupContainer newSortableHeader(String borderId, String property,
                        ISortStateLocator locator) {
                    return new ListTableHeader(borderId, property, locator, ListDataTable.this, triState);
                }
            });
        }
        addBottomToolbar(new ListNavigationToolBar(this, pagingDefinition));

        setItemReuseStrategy(new ManagedReuseStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public void destroyItem(Item item) {
                ListDataTable.this.destroyItem(item);
            }
        });
    }

    public void init(IPluginContext context) {
        this.context = context;
        this.dirty = new HashSet<Item>();
        this.observers = new HashMap<Item, IObserver>();
    }

    public void destroy() {
        for (IObserver observer : observers.values()) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers = null;
        context = null;
        dirty = null;
    }

    public void render(PluginRequestTarget target) {
        if (target != null) {
            for (Item item : dirty) {
                target.addComponent(item);
            }
        }
        dirty.clear();
    }

    @Override
    protected void onBeforeRender() {
        // check if we're on the correct page
        if (!doesPageContainModel(getCurrentPage())) {
            for (int i = 0; i < getPageCount(); i++) {
                if (doesPageContainModel(i)) {
                    setCurrentPage(i);
                    break;
                }
            }
        }

        super.onBeforeRender();
    }

    private boolean doesPageContainModel(int page) {
        IModel selected = getModel();
        int count = getRowsPerPage();
        int offset = page * getRowsPerPage();
        if (offset + count > provider.size()) {
            count = provider.size() - offset;
        }
        Iterator iter = provider.iterator(offset, count);
        while (iter.hasNext()) {
            IModel model = provider.model(iter.next());
            if (model.equals(selected)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Item newRowItem(String id, int index, final IModel model) {
        final OddEvenItem item = new OddEvenItem(id, index, model);
        item.setOutputMarkupId(true);

        item.add(new AttributeAppender("class", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                IModel selected = ListDataTable.this.getModel();
                if (selected != null && selected.equals(model)) {
                    return "hippo-list-selected";
                } else {
                    return null;
                }
            }

            public void setObject(Object object) {
                throw new UnsupportedOperationException();
            }

            public void detach() {
            }
        }, " "));

        item.add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                selectionListener.selectionChanged(model);
            }
        });

        if (context != null && model instanceof IObservable) {
            IObserver observer = new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return (IObservable) model;
                }

                public void onEvent(IEvent event) {
                    dirty.add(item);
                }
            };
            observers.put(item, observer);
            context.registerService(observer, IObserver.class.getName());
        }

        return item;
    }

    protected void destroyItem(Item item) {
        if (context != null) {
            if (observers.containsKey(item)) {
                IObserver observer = observers.remove(item);
                context.unregisterService(observer, IObserver.class.getName());
            }
        }
    }

    public TableSelectionListener getSelectionListener() {
        return selectionListener;
    }
}
