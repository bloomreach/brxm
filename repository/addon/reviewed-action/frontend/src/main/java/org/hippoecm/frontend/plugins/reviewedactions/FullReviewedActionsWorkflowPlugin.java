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
package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.markup.html.panel.Panel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FullReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    @SuppressWarnings("unused")
    private String caption = "unknown document";
    private String stateSummary = "UNKNOWN";
    private boolean isLocked = false;
    private Component locked;

    public FullReviewedActionsWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Label("caption", new PropertyModel(this, "caption")));

        add(new Label("status", new PropertyModel(this, "stateSummary")));

        add(locked = new org.apache.wicket.markup.html.WebMarkupContainer("locked"));

        onModelChanged();

        addWorkflowAction("edit-dialog", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !isLocked;
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IEditService editor = getPluginContext().getService(
                        getPluginConfig().getString(IEditService.EDITOR_ID), IEditService.class);
                if (editor != null) {
                    editor.edit(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
            }
        });

        addWorkflowAction("publish-dialog", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));

            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
            }
        });

        addWorkflowAction("dePublish-dialog", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new"));
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
            }
        });

        addWorkflowAction("delete-dialog", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();

                WorkflowsModel model = (WorkflowsModel) getModel();

                    Node node = model.getNodeModel().getNode();
                    if (node.isNodeType(HippoNodeType.NT_DOCUMENT))
                        node = node.getParent();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        /* FIXME: It requires a discussion if this is truely an operation which we want.  An actual document
                         * delete is not an operation on the document itself, so nothing for the reviewed-actions workflow.
                         * In itself the procedure below looks sound, call archive on the document, however it leads to
                         * undesirable events if this user is allowed to delete the document (based on the embedded
                         * workflow) but is not allowed to see the presence of other variants (such as other languages).
                         * This somewhat seems to be undesirable situation anyway.
                         */
                        boolean isEmpty = true;
                        for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext(); ) {
                            Node child = iter.nextNode();
                            if(child.isNodeType(HippoNodeType.NT_DOCUMENT))
                                isEmpty = false;
                            break;
                        }
                        if(isEmpty) {
                            Node ancestor = node.getParent();
                            while (ancestor != null && !ancestor.isNodeType(HippoNodeType.NT_HANDLE) &&
                                                       !ancestor.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                                                       !ancestor.isNodeType("rep:root")) {
                                ancestor = ancestor.getParent();
                            }
                            if (ancestor != null && ancestor.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                String relPath = node.getPath().substring(ancestor.getPath().length()+1);
                                HippoWorkspace workspace = (HippoWorkspace) ancestor.getSession().getWorkspace();
                                Workflow ancestorWorkflow = workspace.getWorkflowManager().getWorkflow("embedded", ancestor);
                                if(ancestorWorkflow instanceof FolderWorkflow) {
                                    ((FolderWorkflow)ancestorWorkflow).archive(relPath);
                                }
                            }
                        }
                    }
            }});
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            Node node = model.getNodeModel().getNode();
            caption = ISO9075Helper.decodeLocalName(node.getName());

            Node child = null;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext(); ) {
                    child = iter.nextNode();
                    if(child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        node = child;
                        if(child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").getString().equals("draft")) {
                            break;
                        }
                    } else {
                        child = null;
                    }
                }
            }
            if (child != null && child.hasProperty("hippostd:stateSummary")) {
                stateSummary = node.getProperty("hippostd:stateSummary").getString();
            }
            isLocked = false;
            locked.setVisible(isLocked);
            if (child != null && child.hasProperty("hippostd:state") &&
                child.getProperty("hippostd:state").getString().equals("draft") && child.hasProperty("hippostd:holder") &&
                !child.getProperty("hippostd:holder").getString().equals(child.getSession().getUserID())) {
                isLocked = true;
                locked.setVisible(isLocked);
            }
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
            log.info(ex.getClass().getName()+": "+ex.getMessage());
        }

    }
}
