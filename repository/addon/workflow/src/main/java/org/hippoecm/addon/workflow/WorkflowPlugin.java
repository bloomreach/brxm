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
package org.hippoecm.addon.workflow;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Session;

import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);
    
    public static final String CATEGORIES = "workflow.categories";
    
    private String[] categories;
    private final IModelReference modelReference;
    private List<Menu> menu = new LinkedList<Menu>();

    public WorkflowPlugin(IPluginContext context, IPluginConfig config) {
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
            log.warn("No categories ({}) defined", CATEGORIES);
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            modelReference = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelReference.class);
            if (modelReference != null) {
                //updateModel(modelReference.getModel());
                context.registerService(new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return modelReference;
                    }

                    public void onEvent(IEvent event) {
                        if (event instanceof IModelReference.IModelChangeEvent) {
                            IModelReference.IModelChangeEvent<JcrNodeModel> mce = (IModelReference.IModelChangeEvent<JcrNodeModel>)event;
                            updateModel(mce.getNewModel());
                        }
                    }
                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
            log.warn("No model configured");
        }

        menu = new LinkedList<Menu>();
        onModelChanged();
        add(new MenuBar("menu", menu));
    }

    @Override
    protected void onModelChanged() {
        if(getModel() instanceof JcrNodeModel) {
            Node node = ((JcrNodeModel)getModel()).getNode();
            if(node != null) {
                //node.getPath();
            }
        }
        menu.clear();
        try {
            javax.jcr.Session session = ((UserSession)Session.get()).getJcrSession();
            Node document = session.getRootNode().getNode("content/articles/myarticle1/myarticle1");
            Workspace workspace = session.getWorkspace();
            Map<String, WorkflowDescriptor> workflows = new LinkedHashMap<String, WorkflowDescriptor>();
            if (workspace instanceof HippoWorkspace) {
                WorkflowManager workflowMgr = ((HippoWorkspace)workspace).getWorkflowManager();
                for (String category : categories) {
                    WorkflowDescriptor descriptor = workflowMgr.getWorkflowDescriptor(category, document);
                    if (descriptor != null) {
                        workflows.put(category, descriptor);
                    }
                }
            }
            Map<String, List<WorkflowDetail>> items = new LinkedHashMap<String, List<WorkflowDetail>>();
            for (Map.Entry<String, WorkflowDescriptor> entry : workflows.entrySet()) {
                LinkedList<WorkflowDetail> details = new LinkedList<WorkflowDetail>();
                WorkflowDescriptor descriptor = entry.getValue();
                try {
                    Class<Workflow>[] interfaces = descriptor.getInterfaces();
                    for (int i = 0; i < interfaces.length && i < 1; i++) {
                        for (Method method : interfaces[i].getDeclaredMethods()) {
                            details.add(new WorkflowDetail(descriptor, method));
                        }
                        items.put(entry.getKey(), details);
                    }
                } catch (ClassNotFoundException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
            for (Map.Entry<String, List<WorkflowDetail>> item : items.entrySet()) {
                List<Menu> subs = new LinkedList<Menu>();
                for (WorkflowDetail detail : item.getValue()) {
                    subs.add(new WorkflowMenu(detail));
                }
                menu.add(new Menu<Menu>(item.getKey(), subs));
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    public static class WorkflowMenu extends Menu {
        public WorkflowMenu(WorkflowDetail detail) {
            super(detail.getName());
        }
    }

    public static class WorkflowDetail {
        transient WorkflowDescriptor descriptor;
        transient Method method;

        public WorkflowDetail(WorkflowDescriptor descriptor, Method method) {
            this.descriptor = descriptor;
            this.method = method;
        }

        public String getName() {
            String methodName = method.getName();
            Class[] params = method.getParameterTypes();
            for (Class cls : params) {
                methodName += "-" + cls.getName();
            }
            return methodName;
        }
    }
}
