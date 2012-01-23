/*
 *  Copyright 2009 Hippo.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWorkflowPlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";

    private List<IObserver<JcrNodeModel>> observers;
    private PluginController plugins;
    private String[] categories;
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
            categories = new String[] {};
            log.warn("No categories ({}) defined", CATEGORIES);
        }
        IServiceReference serviceReference = context.getReference(this);
        plugins = new PluginController(context, config, serviceReference.getServiceId());
        observers = new LinkedList<IObserver<JcrNodeModel>>();
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
                        Session session = ((UserSession) getSession()).getJcrSession();
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
        final MenuHierarchy menu = new MenuHierarchy();
        plugins.stopRenderers();
        IPluginContext context = getPluginContext();
        for (IObserver<JcrNodeModel> observer : new ArrayList<IObserver<JcrNodeModel>>(observers)) {
            context.unregisterService(observer, IObserver.class.getName());
        }
        observers.clear();
        List<Panel> list = new LinkedList<Panel>();
        for(Node documentNode : nodeSet) {
            if (documentNode != null) {
                try {
                    Workspace workspace = documentNode.getSession().getWorkspace();
                    if (workspace instanceof HippoWorkspace) {
                        WorkflowManager workflowMgr = ((HippoWorkspace) workspace).getWorkflowManager();
                        for (final String category : categories) {
                            final WorkflowDescriptor descriptor = workflowMgr.getWorkflowDescriptor(category, documentNode);
                            if (descriptor != null) {
                                final String pluginRenderer = descriptor.getAttribute(FrontendNodeType.FRONTEND_RENDERER);
                                Panel plugin = null;
                                WorkflowDescriptorModel pluginModel = new WorkflowDescriptorModel(descriptor, category, documentNode);
                                try {
                                    if (pluginRenderer == null || pluginRenderer.trim().equals("")) {
                                        plugin = new StdWorkflowPlugin("item", pluginModel);
                                    } else if(pluginRenderer.startsWith("/")) {
                                        JcrPluginConfig jcrPluginConfig = new JcrPluginConfig(new JcrNodeModel(
                                                documentNode.getSession().getRootNode().getNode(
                                                        pluginRenderer.substring(1))));
                                        JavaPluginConfig filtered = new JavaPluginConfig(jcrPluginConfig);
                                        filtered.put("wicket.model", "${cluster.id}.model}");
                                        plugin = (Panel) plugins.startRenderer(filtered);
                                        if(plugin != null) {
                                            plugin.setDefaultModel(pluginModel);
                                        } else {
                                            log.error("No plugin found on {}",pluginRenderer);
                                        }
                                    } else {
                                        Class pluginClass = Class.forName(pluginRenderer);
                                        if(Panel.class.isAssignableFrom(pluginClass)) {
                                            plugin = (Panel) pluginClass.getConstructor(new Class[]{String.class, WorkflowDescriptorModel.class}).newInstance(new Object[]{"item", pluginModel});
                                            plugin.setDefaultModel(pluginModel);
                                        } else {
                                            plugin = new Panel("id");
                                        }
                                    }
                                    if (plugin != null) {
                                        final JcrNodeModel nodeModel = new JcrNodeModel(documentNode);
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

                                        plugin.beforeRender();

                                        plugin.visitChildren(new MenuVisitor(menu, category, pluginRenderer));
                                        plugin.setVisible(false);
                                        list.add(plugin);
                                    }
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
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Error setting up workflow menu", ex);
                }
            }
        }

        addOrReplace(view = new PanelView(list));
        view.populate();
        view.setVisible(false);

        return menu;
    }

    private static class MenuVisitor implements IVisitor {

        private final MenuHierarchy menu;
        private final String category;
        private final String pluginRenderer;

        public MenuVisitor(final MenuHierarchy menu, final String category, final String pluginRenderer) {
            this.menu = menu;
            this.category = category;
            this.pluginRenderer = pluginRenderer;
        }

        public Object component(Component component) {
            if (component instanceof ActionDescription) {
                menu.put(new String[] {category, pluginRenderer, ((ActionDescription)component).getId()}, (ActionDescription)component);
            } else if (component instanceof MenuDescription) {
                menu.put(category, (MenuDescription) component);
            }
            return IVisitor.CONTINUE_TRAVERSAL;
        }
    }

    private static class PanelView extends AbstractView<Panel> {
        private static final long serialVersionUID = 1L;

        public PanelView(final List<Panel> list) {
            super("view", new ListDataProvider<Panel>(list));
        }

        @Override
        protected void populateItem(Item<Panel> item) {
            item.add(item.getModelObject());
        }
    }
}
