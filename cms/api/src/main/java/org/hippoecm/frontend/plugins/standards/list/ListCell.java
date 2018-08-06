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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;

class ListCell extends Panel {

    private final List<IObserver> observers;
    private final IPluginContext context;

    public ListCell(String id, final IModel model, IListCellRenderer renderer, Object attributeModifier,
            IPluginContext context) {
        super(id, model);

        if ((attributeModifier != null) && !(attributeModifier instanceof AbstractListAttributeModifier)) {
            throw new IllegalArgumentException("attribute modifier must be of type AbstractListAttributeModifier");
        }

        this.context = context;
        this.observers = new LinkedList<>();

        setOutputMarkupId(true);

        if (renderer == null) {
            renderer = new NameRenderer();
        }
        add(renderer.getRenderer("renderer", model));
        final IObservable observable = renderer.getObservable(model);
        if (context != null && observable != null) {
            IObserver observer = new Observer(observable) {

                public void onEvent(Iterator events) {
                    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        target.add(ListCell.this);
                    }
                }

            };
            context.registerService(observer, IObserver.class.getName());
            observers.add(observer);
        }

        if (attributeModifier != null) {
            final AttributeModifier[] cellModifiers = ((AbstractListAttributeModifier) attributeModifier).getCellAttributeModifiers(model);
            if (cellModifiers != null) {
                for (final AttributeModifier cellModifier : cellModifiers) {
                    if (cellModifier == null) {
                        continue;
                    }
                    add(cellModifier);
                    if (context != null && (cellModifier instanceof IObservable)) {
                        IObserver observer = new IObserver<IObservable>() {

                            public IObservable getObservable() {
                                return (IObservable) cellModifier;
                            }

                            public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                                AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                                if (target != null) {
                                    target.add(ListCell.this);
                                }
                            }

                        };
                        context.registerService(observer, IObserver.class.getName());
                        observers.add(observer);
                    }
                }
            }
        }
    }

    @Override
    protected void onDetach() {
        for (IObserver observer : observers) {
            IObservable observable = observer.getObservable();
            if (observable instanceof IDetachable) {
                ((IDetachable) observable).detach();
            }
        }
        super.onDetach();
    }

    @Override
    protected void onRemove() {
        for (IObserver observer : observers) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers.clear();
        super.onRemove();
    }
}
