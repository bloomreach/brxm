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

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
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
import org.hippoecm.frontend.plugins.standards.NodeFilter;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentActivityPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);
    private static final int DEFAULT_LIMIT = 15;

    private final int limit;
    private final List<NodeFilter> filters = new ArrayList<>();

    public CurrentActivityPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (!(getDefaultModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs a model that is an IDataProvider.");
        }

        limit = config.getAsInteger("limit", DEFAULT_LIMIT);

        filters.add(new DefaultNodeFilter());
        for (IPluginConfig childConfig : config.getPluginConfigSet()) {
            if (childConfig.getName().endsWith(".filter")) {
                final String className = childConfig.getString("className");
                try {
                    NodeFilter filter = (NodeFilter) Class.forName(className).newInstance();
                    filters.add(filter);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                    log.error("Can't create NodeFilter {}", className, e);
                }
            }
        }

        add(new CurrentActivityView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    protected boolean accept(JcrNodeModel nodeModel) {
        for (NodeFilter filter : filters) {
            if (!filter.accept(nodeModel)) {
                return false;
            }
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

                private final Iterator upstream = dataProvider.iterator(0,0);
                private Object next = null;
                private int fetched = 0;

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
                    if ("rename".equals(label.getMethodName())) {
                        String[] arguments = label.getArguments();
                        if (arguments != null && arguments.length > 1) {
                            target = new BrowseLinkTarget(path+"/"+arguments[1]);
                        }
                    }
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

    /**
     * We only display messages for 'top-level' workflow items and we don't display
     * actions of system users.
     */
    private static class DefaultNodeFilter implements NodeFilter, Serializable {

        @Override
        public boolean accept(final JcrNodeModel nodeModel) {
            final Node node = nodeModel.getNode();
            try {
                return isValidUser(node) && isWorkflowOrLoginEvent(node) && isTopLevelEvent(node);
            } catch (RepositoryException ignored) {
            }
            return false;
        }

        private boolean isTopLevelEvent(final Node node) throws RepositoryException {
            final String interaction = JcrUtils.getStringProperty(node, "hippolog:interaction", null);
            if (interaction != null) {
                final String category = JcrUtils.getStringProperty(node, "hippolog:workflowCategory", null);
                final String workflowName = JcrUtils.getStringProperty(node, "hippolog:workflowName", null);
                final String methodName = JcrUtils.getStringProperty(node, "hippolog:methodName", null);
                return interaction.equals(category + ":" + workflowName + ":" + methodName);
            }
            return true;
        }

        private boolean isWorkflowOrLoginEvent(final Node node) throws RepositoryException {
            final String category = JcrUtils.getStringProperty(node, "hippolog:category", null);
            if (category != null) {
                switch (category) {
                    case HippoEventConstants.CATEGORY_WORKFLOW:
                    case HippoEventConstants.CATEGORY_SECURITY:
                        return true;
                }
            }
            return false;
        }

        private boolean isValidUser(final Node node) throws RepositoryException {
            final String userName = JcrUtils.getStringProperty(node, "hippolog:user", null);
            if (userName != null) {
                final SecurityService securityService = ((HippoWorkspace) node.getSession().getWorkspace()).getSecurityService();
                if (securityService.hasUser(userName)) {
                    return !securityService.getUser(userName).isSystemUser();
                }
            }
            return false;
        }

    }

}
