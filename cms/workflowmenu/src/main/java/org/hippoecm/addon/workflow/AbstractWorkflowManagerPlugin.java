/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.addon.workflow.categories.CategoriesServiceImpl;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.RepositoryRuntimeException;
import org.hippoecm.frontend.editor.IFormService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.categories.CategoriesService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWorkflowManagerPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(AbstractWorkflowManagerPlugin.class);

    public static final String OBSERVATION_KEY = "workflow.observation";
    public static final String CATEGORIES_KEY = "workflow.categories";
    public static final String VERSION_CATEGORIES_KEY = "workflow.version.categories";
    public static final String XPAGE_CATEGORIES_KEY = "workflow.xpage.categories";
    public static final String MENU_ORDER_KEY = "workflow.menuorder";

    private Set<NodeObserver> observers;
    private PluginController plugins;
    private String[] categories;
    private String[] versionCategories;
    private String[] xpageCategories;
    private String[] menuOrder;
    protected AbstractView view;

    protected AbstractWorkflowManagerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.get(CATEGORIES_KEY) != null) {
            categories = config.getStringArray(CATEGORIES_KEY);
            if (log.isDebugEnabled()) {
                final StringBuilder sb = new StringBuilder("workflow showing categories");
                for (String category : categories) {
                    sb.append(" ");
                    sb.append(category);
                }
                log.debug(sb.toString());
            }
        } else {
            categories = new String[]{};
            log.warn("No categories ({}) defined", CATEGORIES_KEY);
        }

        if (config.get(MENU_ORDER_KEY) != null) {
            menuOrder = config.getStringArray(MENU_ORDER_KEY);
        } else {
            menuOrder = categories;
        }

        if (config.get(VERSION_CATEGORIES_KEY) != null) {
            versionCategories = config.getStringArray(VERSION_CATEGORIES_KEY);
        } else {
            versionCategories = categories;
        }

        if (config.get(XPAGE_CATEGORIES_KEY) != null) {
            xpageCategories = config.getStringArray(XPAGE_CATEGORIES_KEY);
        } else {
            xpageCategories = categories;
        }

        IServiceReference serviceReference = context.getReference(this);
        plugins = new PluginController(context, config, serviceReference.getServiceId());
        if (config.getAsBoolean(OBSERVATION_KEY, true)) {
            observers = new HashSet<>();
        }

        add(new WebMarkupContainer("view") {{
            add(new ListView<Object>("id") {
                @Override
                protected void populateItem(final ListItem<Object> item) {
                }
            });
        }});
        add(new EmptyPanel("menu"));


    }

    protected boolean isObserving() {
        return observers != null;
    }

    @Override
    protected String getBundleName() {
        return "hippo:workflows";
    }

    @Override
    protected void onDetach() {
        if (observers != null) {
            for (IObserver<JcrNodeModel> observer : observers) {
                observer.getObservable().detach();
            }
        }
        super.onDetach();
    }

    protected MenuHierarchy buildMenu(Set<Node> nodeSet, IPluginConfig config) {
        Form form = getForm();

        final MenuHierarchy menu = new MenuHierarchy(Arrays.asList(categories), Arrays.asList(menuOrder), form, config);
        plugins.stopRenderers();

        stopObservation();

        List<Panel> list = new LinkedList<>();
        for (Node node : nodeSet) {
            try {
                list.addAll(buildPanelsForCategories(menu, node, getCategories(node)));
            } catch (RepositoryException ex) {
                throw new RepositoryRuntimeException("Error setting up workflow menu", ex);
            }
        }

        replace(view = new PanelView(list));
        view.populate();
        view.setVisible(false);

        startObservation(nodeSet);

        return menu;
    }

    private String[] getCategories(final Node node) {
        final CategoriesService service = Optional
                .ofNullable(HippoServiceRegistry.getService(CategoriesService.class))
                .orElse(new CategoriesServiceImpl());
        return service.getCategories(node, getPluginContext(), categories, versionCategories, xpageCategories);
    }

    private void stopObservation() {
        if (observers != null) {
            IPluginContext context = getPluginContext();
            for (IObserver<JcrNodeModel> observer : new ArrayList<>(observers)) {
                context.unregisterService(observer, IObserver.class.getName());
            }
            observers.clear();
        }
    }

    private void startObservation(final Set<Node> nodeSet) {
        if (observers != null) {
            IPluginContext context = getPluginContext();

            Set<JcrNodeModel> models = new HashSet<>();
            for (Node node : nodeSet) {
                models.add(new JcrNodeModel(node));
                try {
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        for (Node child : new NodeIterable(node.getNodes())) {
                            models.add(new JcrNodeModel(child));
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Unable to watch document for changes", e);
                }
            }

            for (JcrNodeModel nodeModel : models) {
                NodeObserver observer = new NodeObserver(nodeModel);
                if (!observers.contains(observer)) {
                    observers.add(observer);
                    context.registerService(observer, IObserver.class.getName());
                }
            }
        }
    }

    private Form getForm() {
        final String formServiceId = getPluginConfig().getString("service.form");
        if (formServiceId != null) {
            IFormService formService = getPluginContext().getService(formServiceId, IFormService.class);
            if (formService != null) {
                return formService.getForm();
            }
        }
        return null;
    }

    private List<Panel> buildPanelsForCategories(final MenuHierarchy menu, final Node subject, String[] categories) throws RepositoryException {
        List<Panel> list = new ArrayList<>();
        for (final String category : categories) {
            List<Panel> panels = buildPanelsForCategory(subject, category);
            for (Panel panel : panels) {
                panel.visitChildren(Panel.class, new MenuVisitor(menu, category));
                panel.setVisible(false);
                list.add(panel);
            }
        }
        return list;
    }

    private List<Panel> buildPanelsForCategory(final Node workflowSubject, final String category) throws RepositoryException {
        List<Panel> panels = new LinkedList<>();
        WorkflowDescriptorModel model = new WorkflowDescriptorModel(category, workflowSubject);
        WorkflowDescriptor descriptor = model.getObject();
        if (descriptor == null) {
            // fall back to retrieving workflows on children
            if (workflowSubject.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (Node child : new NodeIterable(workflowSubject.getNodes())) {
                    if (!child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        continue;
                    }
                    panels.addAll(buildPanelsForCategory(child, category));
                }
            }
        } else {
            Panel panel = createPluginForWorkflow(model);
            if (panel != null) {
                panels.add(panel);
            }
        }
        return panels;
    }

    private Panel createPluginForWorkflow(WorkflowDescriptorModel pluginModel) throws RepositoryException {
        WorkflowDescriptor descriptor = pluginModel.getObject();
        String pluginRenderer = descriptor.getAttribute(FrontendNodeType.FRONTEND_RENDERER);
        if (pluginRenderer == null) {
            log.warn("No plugin renderer configured.");
            return null;
        }

        pluginRenderer = pluginRenderer.trim();
        if (!pluginRenderer.startsWith("/")) {
            log.warn("The frontend:pluginrenderer property is no longer supported, only a child node of type frontend:plugin or frontend:plugincluster.");
            return null;
        } else {
            Node node = UserSession.get().getJcrSession().getNode(pluginRenderer);
            final JcrNodeModel nodeModel = new JcrNodeModel(node);
            if (node.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                JcrClusterConfig jcrPluginConfig = new JcrClusterConfig(nodeModel);
                return (Panel) plugins.startRenderer(jcrPluginConfig, pluginModel);
            } else {
                JcrPluginConfig jcrPluginConfig = new JcrPluginConfig(nodeModel);
                return (Panel) plugins.startRenderer(jcrPluginConfig, pluginModel);
            }
        }
    }

    private static class PanelView extends AbstractView<Panel> {
        private static final long serialVersionUID = 1L;

        public PanelView(final List<Panel> list) {
            super("view", new ListDataProvider<>(list));
        }

        @Override
        protected void populateItem(Item<Panel> item) {
            item.add(item.getModelObject());
        }
    }

    private class NodeObserver implements IObserver<JcrNodeModel> {

        private final JcrNodeModel nodeModel;

        public NodeObserver(final JcrNodeModel nodeModel) {
            this.nodeModel = nodeModel;
        }

        public JcrNodeModel getObservable() {
            return nodeModel;
        }

        public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
            modelChanged();
        }

        @Override
        public int hashCode() {
            return 127 * nodeModel.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof NodeObserver)) {
                return false;
            }
            NodeObserver that = (NodeObserver) obj;
            return that.nodeModel.equals(nodeModel);
        }
    }
}
