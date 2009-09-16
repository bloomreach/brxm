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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionPane extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    transient Logger log = LoggerFactory.getLogger(VersionPane.class);
    Label documentComponent;
    Label versionComponent;
    Label createdComponent;
    Label expiredComponent;
    Label labeledComponent;
    Label infoComponent;
    AjaxLink testComponent;
    AjaxLink restoreComponent;
    AjaxLink compareComponent;
    AjaxLink olderComponent;
    AjaxLink newerComponent;
    ModelReference subModel;
    boolean visible = false;

    public VersionPane(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.get("wicket.submodel") != null) {
            subModel = new ModelReference(config.getString("wicket.submodel"), null);
            subModel.init(context);
        } else {
            log.warn("");
        }

        add(documentComponent = new Label("document"));
        add(createdComponent = new Label("created"));
        add(expiredComponent = new Label("expired"));
        add(labeledComponent = new Label("labeled"));
        add(versionComponent = new Label("version"));
        add(infoComponent = new Label("info"));
        add(restoreComponent = new AjaxLink("restore") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                restoreVersion();
            }
        });
        add(olderComponent = new AjaxLink("older") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseVersion(-1);
            }
        });
        add(newerComponent = new AjaxLink("newer") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseVersion(+1);
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
    public Component getComponent() {
        if (visible) {
            return this;
        } else {
            return new Label(getId(), "No document selected");
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        JcrNodeModel model = (JcrNodeModel) getModel();
        visible = false;
        if (model != null && model.getNode() != null) {
            try {
                Node modelNode = model.getNode();
                if (model.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                            if (child.hasProperty("hippostd:state")
                                    && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
                if (modelNode.isNodeType(HippoNodeType.NT_DOCUMENT) && !modelNode.isNodeType("hippostd:folder")) {
                    documentComponent.setModel(new Model(NodeNameCodec.decode(modelNode.getName())));
                    infoComponent.setModel(new Model("This is the current document"));
                    versionComponent.setModel(new Model(""));
                    subModel.setModel(new JcrNodeModel(modelNode));
                    createdComponent.setModel(new Model(""));
                    expiredComponent.setModel(new Model(""));
                    labeledComponent.setModel(new Model(""));
                    visible = true;
                } else {
                    subModel.setModel(new JcrNodeModel((Node) null));
                }
            } catch (RepositoryException ex) {
            }
        }
        reregister();
        redraw();
    }

    private void restoreVersion() {
        JcrNodeModel model = (JcrNodeModel) VersionPane.this.getModel();
        if (model != null) {
            Object currentVersionObject = versionComponent.getModel().getObject();
            int currentVersion = (currentVersionObject instanceof Integer ? ((Integer) currentVersionObject).intValue()
                    : -1);
            Node modelNode = model.getNode();
            try {
                if (model.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                            if (child.hasProperty("hippostd:state")
                                    && child.getProperty("hippostd:state").equals("published")) {
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
                WorkflowManager workflowManager = ((HippoWorkspace) document.getSession().getWorkspace())
                        .getWorkflowManager();
                VersionWorkflow workflow = (VersionWorkflow) workflowManager.getWorkflow("versioning", document);
                if (workflow != null) {
                    SortedMap<Calendar, Set<String>> versions = workflow.list();
                    if (versions.size() == 0)
                        return;
                    Iterator iter = versions.entrySet().iterator();
                    if (currentVersion < 0 || currentVersion >= versions.size())
                        return;
                    for (int i = 0; i < currentVersion; i++)
                        iter.next();
                    Map.Entry<Calendar, Set<String>> entry = (Map.Entry<Calendar, Set<String>>) iter.next();
                    BasicReviewedActionsWorkflow restoreWorkflow = (BasicReviewedActionsWorkflow) workflowManager.getWorkflow("default", document);

                    /*
                     * Disabled, workflow method no longer exists
                     */
                    // restoreWorkflow.restore(entry.getKey());

                    redraw();
                    /* [BvH] Below is a forcefully refresh of the selected
                     * node, without the node being deselected (as with a
                     * flush of the parent).  This because the node is no
                     * longer valid, as it is replaced by an older version
                     * from the version store.  The node however still has the
                     * same UUID and path, just the current instance can no
                     * longer be persisted (but can be read).
                     */
                    IPluginContext context = getPluginContext();
                    IBrowseService browseService = context.getService(IBrowseService.class.getName(),
                            IBrowseService.class);
                    if (browseService != null) {
                        browseService.browse(new JcrNodeModel("/"));
                        browseService.browse(new JcrNodeModel(model.getNode().getPath()));
                    }
                }
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            }
        }
    }

    private void browseVersion(int direction) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        JcrNodeModel model = (JcrNodeModel) VersionPane.this.getModel();
        if (model != null) {
            Object currentVersionObj = versionComponent.getModel().getObject();
            int currentVersion = (currentVersionObj instanceof Integer ? ((Integer) currentVersionObj).intValue() : -1);
            Node modelNode = model.getNode();
            try {
                if (model.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    for (NodeIterator iter = modelNode.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child.getName().equals(modelNode.getName())) {
                            // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                            if (child.hasProperty("hippostd:state")
                                    && child.getProperty("hippostd:state").equals("published")) {
                                modelNode = child;
                                break;
                            } else {
                                modelNode = child;
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage());
            }
            Node document = modelNode;
            try {
                WorkflowManager workflowManager = ((HippoWorkspace) document.getSession().getWorkspace())
                        .getWorkflowManager();
                VersionWorkflow workflow = (VersionWorkflow) workflowManager.getWorkflow("versioning", document);
                if (workflow != null) {
                    SortedMap<Calendar, Set<String>> versions = workflow.list();
                    if (versions.size() == 0)
                        return;
                    Iterator iter = versions.entrySet().iterator();
                    if (currentVersion < 0)
                        currentVersion = versions.size();
                    currentVersion += direction;
                    if (currentVersion >= versions.size())
                        currentVersion = versions.size() - 1;
                    if (currentVersion < 0)
                        currentVersion = 0;
                    versionComponent.setModel(new Model(new Integer(currentVersion)));
                    for (int i = 0; i < currentVersion; i++)
                        iter.next();
                    Map.Entry<Calendar, Set<String>> entry = (Map.Entry<Calendar, Set<String>>) iter.next();
                    Date date = entry.getKey().getTime();
                    createdComponent.setModel(new Model(dateFormat.format(date)));
                    if (iter.hasNext()) {
                        date = ((Map.Entry<Calendar, Set<String>>) iter.next()).getKey().getTime();
                        expiredComponent.setModel(new Model((dateFormat.format(date))));
                    } else
                        expiredComponent.setModel(new Model("present"));
                    StringBuffer labels = new StringBuffer();
                    for (String label : entry.getValue()) {
                        if (labels.length() > 0)
                            labels.append(", ");
                        labels.append(label);
                    }
                    labeledComponent.setModel(new Model(new String(labels)));
                    Document historicDocument = workflow.retrieve(entry.getKey());
                    if (historicDocument == null) {
                        infoComponent.setModel(new Model("There was no document published during this period"));
                        subModel.setModel(null);
                    } else {
                        infoComponent.setModel(new Model(""));
                        subModel.setModel(new JcrNodeModel(document.getSession().getNodeByUUID(
                                historicDocument.getIdentity())));
                    }
                    redraw();
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

    private void reregister() {
        IPluginContext context = getPluginContext();
        String id = getPluginConfig().getString(RenderPlugin.WICKET_ID);
        context.unregisterService(this, id);
        context.registerService(this, id);
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
        WorkflowManager workflowMgr = wsp.getWorkflowManager();
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

}
