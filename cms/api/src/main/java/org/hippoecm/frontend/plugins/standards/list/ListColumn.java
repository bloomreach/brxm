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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;

/**
 * Definition of a column in a {@link ListDataTable}.  Can be used to define sorting,
 * cell renderers and attribute modifiers that will be applied to the repeater {@link Item}.
 * By default, the renderer used is the {@link NameRenderer}, that renders the (translated)
 * name of a JCR node.
 */
public class ListColumn<T> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 1L;

    private Comparator<T> comparator;
    private IListCellRenderer<T> renderer;
    private Object attributeModifier;
    private String cssClass;

    private IPluginContext context;
    private List<IObserver<?>> observers;

    public ListColumn(IModel<String> displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public void setRenderer(IListCellRenderer<T> renderer) {
        this.renderer = renderer;
    }

    public IListCellRenderer<T> getRenderer() {
        return renderer;
    }

    /**
     * Deprecated method to set the list attribute modifier.  Implement the AbstractListAttributeModifier instead.
     */
    @Deprecated
    public void setAttributeModifier(IListAttributeModifier<T> attributeModifier) {
        this.attributeModifier = attributeModifier;
    }

    public void setAttributeModifier(AbstractListAttributeModifier<T> attributeModifier) {
        this.attributeModifier = attributeModifier;
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public IListAttributeModifier<T> getAttributeModifier() {
        if (attributeModifier instanceof IListAttributeModifier) {
            return (IListAttributeModifier<T>) attributeModifier;
        }
        return null;
    }

    void init(IPluginContext context) {
        this.context = context;
        this.observers = new LinkedList<IObserver<?>>();
    }

    void destroy() {
        if (context != null) {
            for (IObserver observer : observers) {
                context.unregisterService(observer, IObserver.class.getName());
            }
            observers.clear();
            context = null;
        }
    }

    @Override
    public void detach() {
        if (observers != null) {
            for (IObserver observer : observers) {
                IObservable observable = observer.getObservable();
                if (observable instanceof IDetachable) {
                    ((IDetachable) observable).detach();
                }
            }
        }
        super.detach();
    }

    protected boolean isLink() {
        return true;
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> model) {
        addLinkBehavior(item, model);

        addCssClasses(item);

        addCell(item, componentId, model);
    }

    protected void addCell(Item<ICellPopulator<T>> item, String componentId, IModel<T> model) {
        final ListCell cell = new ListCell(componentId, model, renderer, attributeModifier, context);
        if (attributeModifier != null) {
            AttributeModifier[] columnModifiers;
            if (attributeModifier instanceof AbstractListAttributeModifier) {
                columnModifiers = ((AbstractListAttributeModifier) attributeModifier).getColumnAttributeModifiers();
            } else {
                columnModifiers = ((IListAttributeModifier) attributeModifier).getColumnAttributeModifiers(model);
            }
            if (columnModifiers != null) {
                for (final AttributeModifier columnModifier : columnModifiers) {
                    if (columnModifier == null) {
                        continue;
                    }
                    item.add(columnModifier);
                    if (columnModifier instanceof IObservable && context != null) {
                        IObserver observer = new IObserver<IObservable>() {

                            public IObservable getObservable() {
                                return (IObservable) columnModifier;
                            }

                            public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                                AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                                if (target != null) {
                                    target.add(cell);
                                }
                            }

                        };
                        context.registerService(observer, IObserver.class.getName());
                        observers.add(observer);
                    }
                }
            }
        }
        item.add(cell);
    }

    protected void addLinkBehavior(final Item<ICellPopulator<T>> item, final IModel<T> model) {
        if (isLink()) {
            item.add(new AjaxEventBehavior("onclick") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    ListDataTable dataTable = item.findParent(ListDataTable.class);
                    dataTable.getSelectionListener().selectionChanged(model);
                }
            });
        }
    }

    protected void addCssClasses(Item<ICellPopulator<T>> item) {
        if (isLink()) {
            item.add(new CssClassAppender(Model.of("link")));
        }
    }

}
