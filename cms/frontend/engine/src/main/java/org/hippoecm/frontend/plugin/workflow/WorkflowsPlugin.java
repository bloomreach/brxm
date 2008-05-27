/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin.workflow;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.EmptyDataProvider;
import org.apache.wicket.markup.repeater.data.DefaultDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.markup.repeater.RefreshingView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class WorkflowsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(WorkflowsPlugin.class);

    RefreshingView view;
    List<String> categories;

    public WorkflowsPlugin(final PluginDescriptor descriptor, IPluginModel model, Plugin parent) {
        super(descriptor, new JcrNodeModel(model), parent);

        categories = descriptor.getParameter("categories").getStrings();
        if(log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("workflow showing categories");
            for(String category : categories)
                sb.append(category);
            log.debug(new String(sb));
        }

        WorkflowsModel workflowsModel = null;
        try {
            workflowsModel = new WorkflowsModel((JcrNodeModel) getModel(), categories);
        } catch(RepositoryException ex) {
            log.error("no workflow available", ex);
        }

        view = new RefreshingView("workflows", workflowsModel) {
                private static final long serialVersionUID = 1L;

            @Override
            protected Iterator getItemModels() {
                return ((WorkflowsModel)getModel()).iterator(0, 1000);
            }

            @Override
            public void populateItem(Item item) {
                WorkflowsModel model = (WorkflowsModel) item.getModel();
                if(log.isDebugEnabled()) {
                    try {
                        log.debug("workflow on "+model.getNodeModel().getNode().getPath() +
                                  " accoring to renderer " + model.getWorkflowName());
                    } catch(RepositoryException ex) {
                        log.debug("debug message failed ", ex);
                    }
                }
                PluginDescriptor descriptor = new PluginDescriptor("workflow", model.getWorkflowName());
                PluginFactory pluginFactory = new PluginFactory(getPluginManager());
                item.add(pluginFactory.createPlugin(descriptor, model, WorkflowsPlugin.this));
            }
        };
        view.setOutputMarkupId(true);
        add(view);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            setPluginModel(model);
            try {
                WorkflowsModel workflowsModel = new WorkflowsModel((JcrNodeModel) getModel(), categories);
                if(log.isDebugEnabled()) {
                    try {
                        log.debug("obtained workflows on "+workflowsModel.getNodeModel().getNode().getPath() +
                                  " counted "+workflowsModel.size()+" unique renderers");
                    } catch(RepositoryException ex) {
                        log.debug("debug message failed ", ex);
                    }
                }
                view.setModel(workflowsModel);
            } catch(RepositoryException ex) {
                log.error("could not setup workflow model", ex);
            }
            notification.getContext().addRefresh(this);
        }
        super.receive(notification);
    }
}
