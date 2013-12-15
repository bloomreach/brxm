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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;
import org.hippoecm.frontend.widgets.ManagedReuseStrategy;

/**
 * A datatable with sorting, pagination, selection notification.  Its columns can be defined
 * with a {@link TableDefinition}.  This component can be used with any data type, i.e. it is
 * not bound to JcrNodeModels.
 */
public class ListDataTable<T> extends DataTable<T, String> {

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private Map<Item<T>, IObserver> observers;
    private Set<Item<T>> dirty;
    private TableDefinition definition;
    private TableSelectionListener<T> selectionListener;
    private final IDataProvider<T> provider;
    private boolean scrollSelectedIntoView = false;
    private boolean scrollSelectedTopAlign = false;

    public interface TableSelectionListener<T> extends IClusterable {

        void selectionChanged(IModel<T> model);
    }

    public ListDataTable(String id, TableDefinition<T> tableDefinition, ISortableDataProvider<T, String> dataProvider,
            TableSelectionListener<T> selectionListener, final boolean triState, IPagingDefinition pagingDefinition) {
        super(id, tableDefinition.getColumns(), dataProvider, pagingDefinition.getPageSize());

        setOutputMarkupId(true);
        setVersioned(false);

        this.definition = tableDefinition;
        this.provider = dataProvider;
        this.selectionListener = selectionListener;

        if (tableDefinition.showColumnHeaders()) {
            addTopToolbar(new AjaxFallbackHeadersToolbar<String>(this, dataProvider) {
                private static final long serialVersionUID = 1L;

                @Override
                protected WebMarkupContainer newSortableHeader(String borderId, String property,
                        ISortStateLocator<String> locator) {
                    return new ListTableHeader<String>(borderId, property, locator) {
                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            target.add(ListDataTable.this);
                        }
                    };
                }
            });

            if (!triState) {
                //Initial sorting on the "Name" column (if any)
                for (IColumn column : getColumns()) {
                    ListColumn<?> listColumn = (ListColumn) column;
                    if (listColumn.getRenderer() == null || listColumn.getRenderer() instanceof NameRenderer) {
                        dataProvider.getSortState().setPropertySortOrder(listColumn.getSortProperty(), SortOrder.ASCENDING);
                        break;
                    }
                }
            }

        }
        addBottomToolbar(new ListNavigationToolBar(this, pagingDefinition));

        setItemReuseStrategy(new ManagedReuseStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public void destroyItem(Item item) {
                ListDataTable.this.destroyItem(item);
            }
        });

        setTableBodyCss("datatable-tbody");
    }

    public void setScrollSelectedIntoView(boolean enabled, boolean topAlign) {
        this.scrollSelectedIntoView = enabled;
        this.scrollSelectedTopAlign = topAlign;
    }

    @Override
    public MarkupContainer setDefaultModel(IModel<?> model) {
        if(observers != null) {
            IModel<?> currentModel = getDefaultModel();
            if (currentModel == null || (model != null && !model.equals(currentModel))) {
                for (Item<T> it : observers.keySet()) {
                    IModel checkModel = it.getModel();
                    if (checkModel.equals(currentModel) || model.equals(checkModel)) {
                        dirty.add(it);
                    }
                }
            }
        }
        return super.setDefaultModel(model);
    }

    @SuppressWarnings("unchecked")
    public IModel<T> getModel() {
        return (IModel<T>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public T getModelObject() {
        return (T) getDefaultModelObject();
    }

    public void setModel(IModel<T> model) {
        setDefaultModel(model);
    }

    public void init(IPluginContext context) {
        this.context = context;
        this.dirty = new HashSet<Item<T>>();
        this.observers = new HashMap<Item<T>, IObserver>();
        definition.init(context);
    }

    public void destroy() {
        if (observers != null && context != null) {
            for (IObserver observer : observers.values()) {
                context.unregisterService(observer, IObserver.class.getName());
            }
        }
        definition.destroy();
        observers = null;
        context = null;
        dirty = null;
    }

    public void render(PluginRequestTarget target) {
        if (target != null && !dirty.isEmpty()) {
            long count = getItemsPerPage();
            long offset = getCurrentPage() * getItemsPerPage();
            if (offset + count > provider.size()) {
                count = provider.size() - offset;
            }

            Set<IModel<T>> visibleModels = new HashSet<IModel<T>>();
            Iterator<? extends T> iter = provider.iterator(offset, count);
            while (iter.hasNext()) {
                IModel<T> model = provider.model(iter.next());
                visibleModels.add(model);
            }
            for (Item item : dirty) {
                if (!visibleModels.contains(item.getModel())) {
                    target.add(this);
                    break;
                }
                target.add(item);
            }
        }
        dirty.clear();
    }

    @Override
    protected void onModelChanged() {
        if (!doesPageContainModel(getCurrentPage())) {
            for (int i = 0; i < getPageCount(); i++) {
                if (doesPageContainModel(i)) {
                    setCurrentPage(i);
                    break;
                }
            }
        }
    }

    private boolean doesPageContainModel(long page) {
        IModel<?> selected = getDefaultModel();
        long count = getItemsPerPage();
        long offset = page * getItemsPerPage();
        if (offset + count > provider.size()) {
            count = provider.size() - offset;
        }
        Iterator<? extends T> iter = provider.iterator(offset, count);
        while (iter.hasNext()) {
            IModel<T> model = provider.model(iter.next());
            if (model.equals(selected)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Item<T> newRowItem(final String id, int index, final IModel<T> model) {
        final OddEvenItem<T> item = new OddEvenItem<T>(id, index, model);
        item.setOutputMarkupId(true);

        item.add(new AttributeAppender("class", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                IModel selected = ListDataTable.this.getDefaultModel();
                if (selected != null && selected.equals(model)) {
                    if (scrollSelectedIntoView) {
                        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                        if (target != null) {
                            target.appendJavaScript("document.getElementById('" + item.getMarkupId()
                                    + "').scrollIntoView(" + scrollSelectedTopAlign + ");");
                        }
                    }
                    return "hippo-list-selected";
                } else {
                    return null;
                }
            }
        }, " "));

        if (context != null && model instanceof IObservable) {
            IObserver observer = newObserver(item, model);
            observers.put(item, observer);
            context.registerService(observer, IObserver.class.getName());
        }

        return item;
    }

    protected final void redrawItem(Item<T> item) {
        dirty.add(item);
    }

    protected IObserver newObserver(final Item<T> item, final IModel<T> model) {
        return new IObserver<IObservable>() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return (IObservable) model;
            }

            public void onEvent(Iterator<? extends IEvent<IObservable>> event) {
                redrawItem(item);
            }
        };
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

    @Override
    protected void onDetach() {
        // check for null; detach is called after component has been removed (destroyed)
        if (observers != null) {
            for (IObserver observer : observers.values()) {
                if (observer instanceof IDetachable) {
                    ((IDetachable) observer).detach();
                }
            }
        }
        super.onDetach();
    }

}
