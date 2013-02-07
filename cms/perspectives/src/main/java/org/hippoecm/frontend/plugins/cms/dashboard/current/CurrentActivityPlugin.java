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
package org.hippoecm.frontend.plugins.cms.dashboard.current;

import java.text.DateFormat;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.plugins.cms.dashboard.DocumentEvent;
import org.hippoecm.frontend.plugins.cms.dashboard.EventModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentActivityPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);
    private static final int DEFAULT_LIMIT = 15;

    private final int limit;

    public CurrentActivityPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (!(getDefaultModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs a model that is an IDataProvider.");
        }

        limit = config.getAsInteger("limit", DEFAULT_LIMIT);

        add(new CurrentActivityView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    /**
     * Filter out event items that are irrelevant.
     * We only display messages for 'top-level' items.
     */
    protected boolean accept(JcrNodeModel nodeModel) {
        final Node node = nodeModel.getNode();
        try {
            final String interaction = JcrUtils.getStringProperty(node, "hippolog:interaction", null);
            if (interaction != null) {
                final String category = JcrUtils.getStringProperty(node, "hippolog:category", null);
                final String workflowName = JcrUtils.getStringProperty(node, "hippolog:workflowName", null);
                final String methodName = JcrUtils.getStringProperty(node, "hippolog:eventMethod", null);
                return interaction.equals(category + ":" + workflowName + ":" + methodName);
            }
        } catch (RepositoryException ignored) {
        }
        return true;
    }

    private class CurrentActivityView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        private final DateFormat dateFormat;

        public CurrentActivityView(String id, IModel model) {
            super(id, model);
            dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, getSession().getLocale());
        }

        @Override
        protected Iterator getItemModels() {
            final IDataProvider dataProvider = (IDataProvider) getDefaultModel();
            return new Iterator() {
                final Iterator upstream = dataProvider.iterator(0,0);
                Object next = null;
                int fetched = 0;
                @Override
                public boolean hasNext() {
                    if (next == null) {
                        fetchNext();
                    }
                    return next != null;
                }

                @Override
                public Object next() {
                    if (next == null) {
                        fetchNext();
                    }
                    if (next == null) {
                        new NoSuchElementException();
                    }
                    final Object result = next;
                    next = null;
                    return result;
                }

                private void fetchNext() {
                    if (fetched >= limit) {
                        return;
                    }
                    while (upstream.hasNext()) {
                        JcrNodeModel candidate = (JcrNodeModel) upstream.next();
                        if (accept(candidate)) {
                            next = candidate;
                            fetched++;
                            break;
                        }
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        protected void populateItem(final Item item) {
            // Add even/odd row css styling
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));

            try {
                final DocumentEvent documentEvent = new DocumentEvent((Node) item.getModelObject());
                final IModel<String> nameModel = documentEvent.getName();
                final EventModel label = new EventModel((JcrNodeModel) item.getModel(), nameModel, dateFormat);
                String path = documentEvent.getDocumentPath();
                if (path != null) {
                    BrowseLinkTarget target = new BrowseLinkTarget(path);
                    BrowseLink link = new BrowseLink(getPluginContext(), getPluginConfig(), "entry", target, label);
                    item.add(link);
                } else {
                    Label entryLabel = new Label("entry", label);
                    entryLabel.setEscapeModelStrings(false);
                    item.add(entryLabel);
                }
            } catch (RepositoryException e) {
                log.error("Failed to create activity event item from log node", e);
            }
        }

    }

}
