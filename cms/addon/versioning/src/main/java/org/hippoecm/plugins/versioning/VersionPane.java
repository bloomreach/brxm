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
package org.hippoecm.plugins.versioning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.SortedMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.frontend.sa.service.IMessageListener;
import org.hippoecm.frontend.sa.service.Message;
import org.hippoecm.frontend.sa.service.render.ModelReference;
import org.hippoecm.frontend.sa.service.render.ModelReference.ModelMessage;
import org.hippoecm.frontend.sa.service.topic.TopicService;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

public class VersionPane extends RenderPlugin
        implements IPlugin, IMessageListener, IClusterable {

    static Logger log = LoggerFactory.getLogger(VersionPane.class);
    private IPluginContext context;
    private IPluginConfig config;
    private TopicService topic;
    private TopicService subtopic;
    DisplayDocument documentComponent;
    Label createdComponent;
    Label expiredComponent;
    Label labeledComponent;
    AjaxLink restoreComponent;
    AjaxLink compareComponent;
    AjaxLink olderComponent;
    AjaxLink newerComponent;

    public VersionPane(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.context = context;
        this.config = config;

        if (config.get(RenderPlugin.MODEL_ID) != null) {
            topic = new TopicService(config.getString(RenderPlugin.MODEL_ID));
            topic.addListener(this);
            topic.init(context);
            subtopic = new TopicService(config.getString("wicket.submodel"));
            subtopic.init(context);
        } else {
            log.warn("");
        }

        add(documentComponent = new DisplayDocument(context, config));
        add(createdComponent = new Label("created"));
        add(expiredComponent = new Label("expired"));
        add(labeledComponent = new Label("labeled"));
        add(restoreComponent = new AjaxLink("restore") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                restoreVersion();
            }
        });

        add(compareComponent = new AjaxLink("compare") {

            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        });

        add(olderComponent = new AjaxLink("older") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseVersion(-1);
            }
        });

        add(newerComponent = new AjaxLink("newer") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseVersion(+1);
            }
        });
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(VersionPane.class);
    }

    public void onMessage(Message message) {
        switch (message.getType()) {
            case ModelReference.SET_MODEL:
                // setModel((JcrNodeModel) ((ModelReference.ModelMessage) message).getModel());
                break;
        }
    }

    public void onConnect() {
    }

    public void onModelChanged() {
        super.onModelChanged();
        JcrNodeModel model = (JcrNodeModel) getModel();
        if (model != null) {
            try {
                if (model.getNode().isNodeType("hippostd:document")) {
                    currentVersion = -1;
                    subtopic.publish(new ModelMessage(ModelReference.SET_MODEL, new JcrNodeModel(model.getNode())));
                }
            } catch (RepositoryException ex) {
            }
        }
    }
    private int currentVersion;

    private void restoreVersion() {
    }

    private void browseVersion(int direction) {
        JcrNodeModel model = (JcrNodeModel) VersionPane.this.getModel();
        if (model != null) {
            Node document = model.getNode();
            try {
                WorkflowManager workflowManager = ((HippoWorkspace) document.getSession().getWorkspace()).getWorkflowManager();
                VersionWorkflow workflow = (VersionWorkflow) workflowManager.getWorkflow("versioning", document);
                if (workflow != null) {
                    SortedMap<Calendar, Set<String>> versions = workflow.list();
                    Iterator<Calendar> iter = versions.keySet().iterator();
                    for(int i=0; i<currentVersion+direction; i++)
                            iter.next();
                    Document historicDocument = workflow.retrieve(iter.next());
                    subtopic.publish(new ModelMessage(ModelReference.SET_MODEL, new JcrNodeModel(document.getSession().getNodeByUUID(historicDocument.getIdentity()))));
                }
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}

