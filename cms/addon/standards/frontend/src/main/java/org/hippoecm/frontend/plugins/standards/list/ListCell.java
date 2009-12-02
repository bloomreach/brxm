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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListCell extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(ListCell.class);

    private List<IObserver> observers;
    private IPluginContext context;

    public ListCell(String id, final IModel model, IListCellRenderer renderer,
            IListAttributeModifier attributeModifier, IPluginContext context) {
        super(id, model);

        this.context = context;
        this.observers = new LinkedList<IObserver>();

        setOutputMarkupId(true);

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            protected CharSequence getEventHandler() {
                return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                ListDataTable dataTable = (ListDataTable) findParent(ListDataTable.class);
                dataTable.getSelectionListener().selectionChanged(model);
            }
        });

        if (renderer == null) {
            add(new NameRenderer().getRenderer("renderer", model));
        } else {
            add(renderer.getRenderer("renderer", model));
        }

        if (attributeModifier != null) {
            AttributeModifier[] cellModifiers = attributeModifier.getCellAttributeModifiers(model);
            if (cellModifiers != null) {
                for (final AttributeModifier cellModifier : cellModifiers) {
                    add(cellModifier);
                    if (cellModifier instanceof IObservable) {
                        IObserver observer = new IObserver<IObservable>() {

                            public IObservable getObservable() {
                                return (IObservable) cellModifier;
                            }

                            public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                                AjaxRequestTarget target = AjaxRequestTarget.get();
                                if (target != null) {
                                    target.addComponent(ListCell.this);
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
    protected void onRemove() {
        for (IObserver observer : observers) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers.clear();
        super.onRemove();
    }
}
