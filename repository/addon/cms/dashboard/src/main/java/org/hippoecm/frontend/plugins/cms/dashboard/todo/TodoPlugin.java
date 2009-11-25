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
package org.hippoecm.frontend.plugins.cms.dashboard.todo;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TodoPlugin.class);

    public TodoPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if (!(getDefaultModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("TodoPlugin needs an IDataProvider as Plugin model.");
        }
        add(new TodoView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    private class TodoView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public TodoView(String id, IModel model) {
            super(id, model);
        }

        @Override
        protected Iterator getItemModels() {
            final IDataProvider dataProvider = (IDataProvider) getDefaultModel();
            final Iterator<Node> iter = dataProvider.iterator(0, 0);
            return new Iterator() {
                private Node next;
                public boolean hasNext() {
                    while (next == null && iter.hasNext()) {
                        next = iter.next();
                        try {
                            if (!next.hasProperty("type")) {
                                next = null;
                            }
                        } catch (RepositoryException ex) {
                            next = null;
                        }
                    }
                    return next != null;
                }
                public Object next() {
                    while (next == null && iter.hasNext()) {
                        next = iter.next();
                        try {
                            if (!next.hasProperty("type")) {
                                next = null;
                            }
                        } catch (RepositoryException ex) {
                            next = null;
                        }
                    }
                    Node rtValue = next;
                    next = null;
                    return dataProvider.model(rtValue);
                }
                public void remove() {
                    throw new UnsupportedOperationException("Unsupported operation");
                }
            };
        }

        @Override
        protected void populateItem(final Item item) {
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));

            String path = "/";
            try {
                Node node = (Node) item.getModelObject();
                path = node.getPath();
            } catch (RepositoryException e) {
                log.warn("unable to find path", e);
            }
            BrowseLinkTarget target = new BrowseLinkTarget(path);
            item.add(new BrowseLink(getPluginContext(), getPluginConfig(), "request", new Model(target),
                    new PropertyModel(target, "name")));

            Request request = new Request((JcrNodeModel) item.getModel(), TodoPlugin.this);
            item.add(new Label("request-description", new PropertyModel(request, "localType")));
            item.add(new Label("request-owner", new PropertyModel(request, "username")));
        }
    }

    static class Request extends JcrObject {
        private static final long serialVersionUID = 1L;

        private Component container;

        Request(JcrNodeModel model, Component container) {
            super(model);
            this.container = container;
        }

        public String getType() {
            try {
                return getNode().getProperty("type").getString();
            } catch (RepositoryException e) {
            }
            return null;
        }

        public String getLocalType() {
            try {
                return new StringResourceModel(getNode().getProperty("type").getString(), container, null).getString();
            } catch (RepositoryException e) {
            }
            return null;
        }

        public String getUsername() {
            try {
                return getNode().getProperty("username").getString();
            } catch (RepositoryException e) {
            }
            return null;
        }

        @Override
        protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
        }
    }
}
