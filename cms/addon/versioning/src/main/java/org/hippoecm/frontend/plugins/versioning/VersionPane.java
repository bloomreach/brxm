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
package org.hippoecm.frontend.plugins.versioning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.model.ModelService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

public class VersionPane extends RenderPlugin {
    transient Logger log = LoggerFactory.getLogger(VersionPane.class);
    Label documentComponent;
    Label versionComponent;
    Label createdComponent;
    Label expiredComponent;
    Label labeledComponent;
    AjaxLink testComponent;
    AjaxLink restoreComponent;
    AjaxLink compareComponent;
    AjaxLink olderComponent;
    AjaxLink newerComponent;
    ModelService subModel;

    public VersionPane(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.get(RenderPlugin.MODEL_ID)!=null) {
            subModel = new ModelService(config.getString("wicket.submodel"), null);
            subModel.init(context);
        } else {
            log.warn("");
        }

        addExtensionPoint("display");
        add(documentComponent = new Label("document"));
        add(versionComponent = new Label("version"));
        add(createdComponent = new Label("created"));
        add(expiredComponent = new Label("expired"));
        add(labeledComponent = new Label("labeled"));
        add(restoreComponent = new AjaxLink("restore") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                restoreVersion();
            }
        });

        /*add(compareComponent = new AjaxLink("compare") {
        @Override
        public void onClick(AjaxRequestTarget target) {
        }
        });*/

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

        add(newerComponent = new AjaxLink("test") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                test();
            }
        });
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(VersionPane.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        JcrNodeModel model = (JcrNodeModel)getModel();
        if (model!=null) {
            try {
                Node modelNode = model.getNode();
                if (model.getNode().isNodeType("hippo:handle")) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            if (child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
                if (modelNode.isNodeType("hippo:document")) {
                    documentComponent.setModel(new Model(modelNode.getName()));
                    versionComponent.setModel(new Model("current"));
                    subModel.setModel(new JcrNodeModel(modelNode));
                    createdComponent.setModel(new Model(""));
                    expiredComponent.setModel(new Model(""));
                    labeledComponent.setModel(new Model(""));
                }
            } catch (RepositoryException ex) {
            }
        }
    }

    private void restoreVersion() {
        JcrNodeModel model = (JcrNodeModel)VersionPane.this.getModel();
        if (model!=null) {
            Object currentVersionObject = versionComponent.getModel().getObject();
            int currentVersion = (currentVersionObject instanceof Integer ? ((Integer)currentVersionObject).intValue() : -1);
            Node modelNode = model.getNode();
            try {
                if (model.getNode().isNodeType("hippo:handle")) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            if (child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
            }

            Node document = modelNode;
            try {
                WorkflowManager workflowManager = ((HippoWorkspace)document.getSession().getWorkspace()).getWorkflowManager();
                VersionWorkflow workflow = (VersionWorkflow)workflowManager.getWorkflow("versioning", document);
                if (workflow!=null) {
                    SortedMap<Calendar, Set<String>> versions = workflow.list();
                    if (versions.size()==0)
                        return;
                    Iterator iter = versions.entrySet().iterator();
                    if (currentVersion<0||currentVersion>=versions.size())
                        return;
                    for (int i = 0; i<currentVersion; i++)
                        iter.next();
                    Map.Entry<Calendar, Set<String>> entry = (Map.Entry<Calendar, Set<String>>)iter.next();
                    workflow.revert(entry.getKey());
                    redraw();
                }
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private void browseVersion(int direction) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        JcrNodeModel model = (JcrNodeModel)VersionPane.this.getModel();
        if (model!=null) {
            Object currentVersionObj = versionComponent.getModel().getObject();
            int currentVersion = (currentVersionObj instanceof Integer ? ((Integer)currentVersionObj).intValue() : -1);
            Node modelNode = model.getNode();
            try {
                if (model.getNode().isNodeType("hippo:handle")) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            if (child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
            }
            Node document = modelNode;
            try {
                WorkflowManager workflowManager = ((HippoWorkspace)document.getSession().getWorkspace()).getWorkflowManager();
                VersionWorkflow workflow = (VersionWorkflow)workflowManager.getWorkflow("versioning", document);
                if (workflow!=null) {
                    SortedMap<Calendar, Set<String>> versions = workflow.list();
                    if (versions.size()==0)
                        return;
                    Iterator iter = versions.entrySet().iterator();
                    if (currentVersion<0)
                        currentVersion = versions.size();
                    currentVersion += direction;
                    if (currentVersion>=versions.size())
                        currentVersion = versions.size()-1;
                    if (currentVersion<0)
                        currentVersion = 0;
                    versionComponent.setModel(new Model(new Integer(currentVersion)));
                    for (int i = 0; i<currentVersion; i++)
                        iter.next();
                    Map.Entry<Calendar, Set<String>> entry = (Map.Entry<Calendar, Set<String>>)iter.next();
                    Date date = entry.getKey().getTime();
                    createdComponent.setModel(new Model(dateFormat.format(date)));
                    if (iter.hasNext()) {
                        date = ((Map.Entry<Calendar, Set<String>>)iter.next()).getKey().getTime();
                        expiredComponent.setModel(new Model((dateFormat.format(date))));
                    } else
                        expiredComponent.setModel(new Model("present"));
                    StringBuffer labels = new StringBuffer();
                    for (String label : entry.getValue()) {
                        if (labels.length()>0)
                            labels.append(", ");
                        labels.append(label);
                    }
                    labeledComponent.setModel(new Model(new String(labels)));
                    Document historicDocument = workflow.retrieve(entry.getKey());
                    subModel.setModel(new JcrNodeModel(document.getSession().getNodeByUUID(
                            historicDocument.getIdentity())));
                    redraw();
                }
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    // FIXME: to be removed after testing
    private void test() {
        JcrNodeModel model = (JcrNodeModel)VersionPane.this.getModel();
        if (model!=null) {
            Node modelNode = model.getNode();
            try {
                if (model.getNode().isNodeType("hippo:handle")) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            if (child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
            }
            Node document = modelNode;
            try {
                publish(edit(document));
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }

    private Node edit(Node node) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        FullReviewedActionsWorkflow publishwf;
        Document document;

        Session session = node.getSession();
        String path = node.getPath();

        // start edit
        publishwf = (FullReviewedActionsWorkflow)getWorkflow(node, "reviewed-action");
        document = publishwf.obtainEditableInstance();
        session.save();
        session.refresh(false);

        // edit
        node = session.getNodeByUUID(document.getIdentity());
        node.setProperty("hippostd:content", node.getProperty("hippostd:content").getString()+"!");
        session.save();
        session.refresh(false);

        // commit edit
        node = session.getNodeByUUID(document.getIdentity());
        publishwf = (FullReviewedActionsWorkflow)getWorkflow(node, "reviewed-action");
        publishwf.commitEditableInstance();
        session.save();
        session.refresh(false);

        return session.getRootNode().getNode(path.substring(1));
    }

    private void publish(Node node) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Session session = node.getSession();
        session.save();
        FullReviewedActionsWorkflow publishwf = (FullReviewedActionsWorkflow)getWorkflow(node, "reviewed-action");
        publishwf.publish();
        session.save();
        session.refresh(false);
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        HippoWorkspace wsp = (HippoWorkspace)node.getSession().getWorkspace();
        WorkflowManager workflowMgr = wsp.getWorkflowManager();
        Node canonicalNode = ((HippoNode)node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }
}
