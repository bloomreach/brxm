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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWorkflowPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";

    private PluginController plugins;
    private String[] categories;
    protected AbstractView view;

    protected AbstractWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        if (config.get(CATEGORIES) != null) {
            categories = config.getStringArray(CATEGORIES);
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("workflow showing categories");
                for (String category : categories)
                    sb.append(" " + category);
                log.debug(new String(sb));
            }
        } else {
            categories = new String[] {};
            log.warn("No categories ({}) defined", CATEGORIES);
        }

        plugins = new PluginController(context, config, context.getReference(this).getServiceId());
    }

    MenuHierarchy buildMenu(Set<Node> nodeSet) {
        plugins.stopRenderers();

        final MenuHierarchy menu = new MenuHierarchy();
        List<Panel> list = new LinkedList<Panel>();
        for(Node documentNode : nodeSet) {
            if (documentNode != null) {
                try {
                    Workspace workspace = documentNode.getSession().getWorkspace();
                    if (workspace instanceof HippoWorkspace) {
                        WorkflowManager workflowMgr = ((HippoWorkspace) workspace).getWorkflowManager();
                        for (final String category : categories) {
                            try {
                                final WorkflowDescriptor descriptor = workflowMgr.getWorkflowDescriptor(category, documentNode);
                                if (descriptor != null) {
                                    String pluginRenderer = descriptor.getAttribute(FrontendNodeType.FRONTEND_RENDERER);
                                    Panel plugin = null;
                                    WorkflowDescriptorModel pluginModel = new WorkflowDescriptorModel(descriptor, category, documentNode);
                                    if (pluginRenderer == null || pluginRenderer.trim().equals("")) {
                                        plugin = new StdWorkflowPlugin("item", pluginModel);
                                    } else if(pluginRenderer.startsWith("/")) {
                                        plugin = (Panel) plugins.startRenderer(new JcrPluginConfig(new JcrNodeModel(documentNode.getSession().getRootNode().getNode(pluginRenderer.substring(1)))));
                                        if(plugin != null) {
                                            plugin.setModel(pluginModel);
                                        } else {
                                            log.error("No plugin found on {}",pluginRenderer);
                                        }
                                    } else {
                                        Class pluginClass = Class.forName(pluginRenderer);
                                        if(Panel.class.isAssignableFrom(pluginClass)) {
                                            plugin = (Panel) pluginClass.getConstructor(new Class[]{String.class, WorkflowDescriptorModel.class}).newInstance(new Object[]{"item", pluginModel});
                                            plugin.setModel(pluginModel);
                                        } else {
                                            plugin = new Panel("id");
                                        }
                                    }
                                    if (plugin != null) {
                                        plugin.visitChildren(new IVisitor() {

                                            public Object component(Component component) {
                                                try {
                                                    if (component instanceof ActionDescription) {
                                                        menu.put(new String[] {category, descriptor.getAttribute(FrontendNodeType.FRONTEND_RENDERER), ((ActionDescription)component).getId()}, (ActionDescription)component);
                                                    }
                                                } catch (RepositoryException ex) {
                                                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                                    ex.printStackTrace(System.err);
                                                }
                                                return IVisitor.CONTINUE_TRAVERSAL;
                                            }
                                        });
                                        plugin.setVisible(false);
                                        list.add(plugin);
                                    }
                                }
                            } catch (ClassNotFoundException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            } catch (NoSuchMethodException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            } catch (InstantiationException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            } catch (IllegalAccessException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            } catch (InvocationTargetException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        }

        addOrReplace(view = new AbstractView("view", new ListDataProvider(list)) {
            @Override
            protected void populateItem(Item item) {
                item.add((Panel) item.getModelObject());
            }
        });
        view.populate();
        view.setVisible(false);

        return menu;
    }

}
