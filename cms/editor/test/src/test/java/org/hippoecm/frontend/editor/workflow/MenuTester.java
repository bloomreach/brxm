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
package org.hippoecm.frontend.editor.workflow;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;

public class MenuTester extends DataView {

    private static final long serialVersionUID = 1L;

    public MenuTester(String id, MarkupContainer component) {
        super(id, getProvider(component));
    }

    static IDataProvider getProvider(final MarkupContainer container) {
        return new IDataProvider() {
            private static final long serialVersionUID = 1L;

            private List<WorkflowAction> list;

            @Override
            public Iterator iterator(final long first, final long count) {
                attach();

                long toIndex = first + count;
                if (toIndex > list.size()) {
                    toIndex = list.size();
                }
                return list.subList((int) first, (int) toIndex).listIterator();
            }

            @Override
            public IModel model(Object object) {
                return new Model((Serializable) object);
            }

            @Override
            public long size() {
                attach();
                return list.size();
            }

            @SuppressWarnings({ "unchecked", "deprecation" })
            void attach() {
                if (list == null) {
                    list = new LinkedList<WorkflowAction>();
                    container.visitChildren(WorkflowAction.class, new IVisitor<WorkflowAction, Void>() {

                        @Override
                        public void component(WorkflowAction component, IVisit<Void> visit) {
                            list.add(component);
                        }
                    });
                }
            }

            @Override
            public void detach() {
                list = null;
            }

        };
    }

    @Override
    protected void populateItem(Item item) {
        final WorkflowAction action = (WorkflowAction) item.getModelObject();
        item.add(new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                action.run();
            }
        });
    }
}
