/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.editor.IFormService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWorkflowPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";
    public static final String MENU_ORDER = "workflow.menuorder";

    private List<IObserver<JcrNodeModel>> observers;
    private PluginController plugins;
    private String[] categories;
    private String[] menuOrder;
    protected AbstractView view;

    protected AbstractWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if (config.get(CATEGORIES) != null) {
            categories = config.getStringArray(CATEGORIES);
            if (log.isDebugEnabled()) {
                final StringBuffer sb = new StringBuffer("workflow showing categories");
                for (String category : categories) {
                    sb.append(" ");
                    sb.append(category);
                }
                log.debug(sb.toString());
            }
        } else {
            categories = new String[]{};
            log.warn("No categories ({}) defined", CATEGORIES);
        }
        if (config.get(MENU_ORDER) != null) {
            menuOrder = config.getStringArray(MENU_ORDER);
        } else {
            menuOrder = categories;
        }
        IServiceReference serviceReference = context.getReference(this);
        plugins = new PluginController(context, config, serviceReference.getServiceId());
        observers = new LinkedList<>();
    }

    @Override
    public String getString(Map<String, String> criteria) {
        String key = criteria.get(HippoNodeType.HIPPO_KEY);
        if (key != null) {
            String language = getLocale().getLanguage();
            for (String category : categories) {
                if (key.equals(category)) {
                    String path = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.WORKFLOWS_PATH + "/" + category;
                    try {
                        Session session = getSession().getJcrSession();
                        if (session.itemExists(path)) {
                            Node node = (Node) session.getItem(path);
                            if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                                NodeIterator translations = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                                while (translations.hasNext()) {
                                    Node translation = translations.nextNode();
                                    if (translation.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString().equals(language)) {
                                        return translation.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }
        }
        return super.getString(criteria);
    }

    @Override
    protected void onDetach() {
        for (IObserver<JcrNodeModel> observer : observers) {
            observer.getObservable().detach();
        }
        super.onDetach();
    }

    MenuHierarchy buildMenu(Set<Node> nodeSet) {
        Form form = getForm();

        final MenuHierarchy menu = new MenuHierarchy(Arrays.asList(categories), Arrays.asList(menuOrder), form);
        plugins.stopRenderers();
        IPluginContext context = getPluginContext();
        for (IObserver<JcrNodeModel> observer : new ArrayList<>(observers)) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers.clear();
        List<Panel> list = new LinkedList<>();
        for (Node node : nodeSet) {
            for (final String category : categories) {
                List<Panel> panels = buildCategory(context, node, category);
                for (Panel panel : panels) {
                    panel.visitChildren(Panel.class, new MenuVisitor(menu, category));
                    panel.setVisible(false);
                    list.add(panel);
                }
            }
        }

        addOrReplace(view = new PanelView(list));
        view.populate();
        view.setVisible(false);

        return menu;
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

    private List<Panel> buildCategory(final IPluginContext context, final Node node, final String category) {
        List<Panel> panels = new LinkedList<>();
        try {
            WorkflowManager workflowMgr = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
            WorkflowDescriptor descriptor = workflowMgr.getWorkflowDescriptor(category, node);
            if (descriptor == null) {
                // fall back to retrieving workflows on children
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    for (Node child : new NodeIterable(node.getNodes())) {
                        if (!child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            continue;
                        }
                        descriptor = workflowMgr.getWorkflowDescriptor(category, child);
                        if (descriptor != null) {
                            Panel panel = buildCategoryForNode(context, child, descriptor, category);
                            if (panel != null) {
                                panels.add(panel);
                            }
                        }
                    }
                }
            } else {
                Panel panel = buildCategoryForNode(context, node, descriptor, category);
                if (panel != null) {
                    panels.add(panel);
                }
            }
        } catch (RepositoryException ex) {
            log.error("Error setting up workflow menu", ex);
        }
        return panels;
    }

    private Panel buildCategoryForNode(final IPluginContext context, final Node node, WorkflowDescriptor descriptor, final String category) throws RepositoryException {
        final String pluginRenderer = descriptor.getAttribute(FrontendNodeType.FRONTEND_RENDERER);
        WorkflowDescriptorModel pluginModel = new WorkflowDescriptorModel(descriptor, category, node);
        Panel plugin = createPlugin(category, pluginRenderer, pluginModel);
        if (plugin == null) {
            return null;
        }

        final JcrNodeModel nodeModel = new JcrNodeModel(node);
        IObserver<JcrNodeModel> observer = new IObserver<JcrNodeModel>() {

            public JcrNodeModel getObservable() {
                return nodeModel;
            }

            public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
                modelChanged();
            }

        };
        observers.add(observer);
        context.registerService(observer, IObserver.class.getName());

        return plugin;

    }

    private Panel createPlugin(final String category, final String pluginRenderer, final WorkflowDescriptorModel pluginModel) throws RepositoryException {
        final Panel plugin;
        try {
            if (pluginRenderer == null || pluginRenderer.trim().equals("")) {
                plugin = new StdWorkflowPlugin("item", pluginModel);
            } else {
                if (pluginRenderer.startsWith("/")) {
                    Node node = UserSession.get().getJcrSession().getNode(pluginRenderer);
                    final JcrNodeModel nodeModel = new JcrNodeModel(node);
                    if (node.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                        JcrClusterConfig jcrPluginConfig = new JcrClusterConfig(nodeModel);
                        plugin = (Panel) plugins.startRenderer(jcrPluginConfig, pluginModel);
                    } else {
                        JcrPluginConfig jcrPluginConfig = new JcrPluginConfig(nodeModel);
                        plugin = (Panel) plugins.startRenderer(jcrPluginConfig, pluginModel);
                    }
                } else {
                    Class pluginClass = Class.forName(pluginRenderer);
                    if (Panel.class.isAssignableFrom(pluginClass)) {
                        Constructor constructor = pluginClass.getConstructor(
                                new Class[]{String.class, WorkflowDescriptorModel.class});
                        plugin = (Panel) constructor.newInstance(new Object[]{"item", pluginModel});
                    } else {
                        log.warn("Invalid plugin class '" + pluginRenderer + "', it does not extend Panel.");
                        return null;
                    }
                }
            }
            return plugin;
        } catch (ClassNotFoundException ex) {
            log.warn("Could not find plugin class '" + pluginRenderer + "' for category '" + category + "'", ex);
        } catch (NoSuchMethodException ex) {
            log.warn("Could not find legacy constructor for '" + pluginRenderer + "' for category '" + category + "'", ex);
        } catch (InstantiationException ex) {
            log.warn("Failed to instantiate '" + pluginRenderer + "' for category '" + category + "'", ex);
        } catch (IllegalAccessException ex) {
            log.warn("Could not access constructor of '" + pluginRenderer + "' for category '" + category + "'", ex);
        } catch (InvocationTargetException ex) {
            log.warn("Plugin '" + pluginRenderer + "' for category '" + category + "' threw exception while initializing", ex);
        }
        return null;
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
}
