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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow plugin which adds non-application programmer accessible
 * functionality to the CMS, allowing all documents in a folder or directory
 * and recursively below the folder or directory to be published or
 * unpublished.
 *
 * This class is NOT part of any API provided and should not be extended by
 * other projects despite having public signature.
 *
 * Errors emanating from bad configuration, unable to query or mid-air
 * conflicts when gathering documents are reported as errors in the log, while
 * non-fatal errors that are caused by documents which can currently not be
 * (un)published are logged as warnings.  This even though they are not really
 * serious, but you do want to keep track of them.
 */
public class ExtendedFolderWorkflowPlugin extends FolderWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ExtendedFolderWorkflowPlugin.class);

    private static final String QUERY_LANGUAGE_PUBLISH = Query.SQL;
    private static final String QUERY_STATEMENT_PUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '$basefolder/%' AND hippostd:state='unpublished'";
    private static final String QUERY_LANGUAGE_DEPUBLISH = Query.SQL;
    private static final String QUERY_STATEMENT_DEPUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '$basefolder/%' AND hippostd:state='published'";
    private static final String WORKFLOW_CATEGORY = "default";

    private String name;
    private int processed;
    private Set<String> documents;

    public ExtendedFolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("publishAll", new StringResourceModel("publish-all-label", this, null)) {

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "publish-all-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    name = ((HippoNode)((WorkflowDescriptorModel)getDefaultModel()).getNode()).getLocalizedName();
                } catch(RepositoryException ex) {
                    name = "";
                }
                documents = new HashSet<String>();
                Session session = ((UserSession)getSession()).getJcrSession();
                Query query = null;
                try {
                    QueryManager qMgr = session.getWorkspace().getQueryManager();
                    query = qMgr.createQuery(QUERY_STATEMENT_PUBLISH, QUERY_LANGUAGE_PUBLISH);
                } catch (RepositoryException ex) {
                    log.error("Error preparing to publish all documents: {}", ex);
                }
                return new ExtendedFolderWorkflowPlugin.ConfirmDialog(this,
                        new StringResourceModel("publish-all-title", ExtendedFolderWorkflowPlugin.this, null),
                        new StringResourceModel("publish-all-text", ExtendedFolderWorkflowPlugin.this, null),
                        new StringResourceModel("publish-all-subtext", ExtendedFolderWorkflowPlugin.this, null),
                        new PropertyModel(ExtendedFolderWorkflowPlugin.this, "name"),
                        documents, query);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                Session session = ((UserSession) getSession()).getJcrSession();
                WorkflowManager wfMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                for (String uuid : documents) {
                    try {
                        Node document = session.getNodeByIdentifier(uuid);
                        String path = document.getPath();
                        if (document.getDepth() > 0) {
                            Node handle = document.getParent();
                            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                                for (NodeIterator requestIter = handle.getNodes(HippoNodeType.NT_REQUEST); requestIter.hasNext(); ) {
                                    Node request = requestIter.nextNode();
                                    if (request != null) {
                                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, request);
                                        if (workflow instanceof FullRequestWorkflow) {
                                            ((FullRequestWorkflow) workflow).cancelRequest();
                                            log.info("removed request(s) from document "+document.getPath()+" ("+uuid+")");
                                        }
                                    }
                                }
                            }
                        }
                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, document);
                        if (workflow instanceof FullReviewedActionsWorkflow) {
                            ((FullReviewedActionsWorkflow) workflow).publish();
                            ++processed;
                            log.info("published document "+path+" ("+uuid+")");
                        }
                    } catch (MappingException ex) {
                        log.warn("Publication of {} failed: {}", uuid, ex);
                    } catch (WorkflowException ex) {
                        log.warn("Publication of {} failed: {}", uuid, ex);
                    } catch (RemoteException ex) {
                        log.warn("Publication of {} failed: {}", uuid, ex);
                    } catch (RepositoryException ex) {
                        log.warn("Publication of {} failed: {}", uuid, ex);
                    }
                    session.refresh(true);
                }
            }
        });

        add(new WorkflowAction("depublishAll", new StringResourceModel("depublish-all-label", this, null)) {

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "depublish-all-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    name = ((HippoNode)((WorkflowDescriptorModel)getDefaultModel()).getNode()).getLocalizedName();
                } catch(RepositoryException ex) {
                    name = "";
                }
                documents = new HashSet<String>();
                Session session = ((UserSession)getSession()).getJcrSession();
                Query query = null;
                try {
                    QueryManager qMgr = session.getWorkspace().getQueryManager();
                    query = qMgr.createQuery(QUERY_STATEMENT_DEPUBLISH, QUERY_LANGUAGE_DEPUBLISH);
                } catch (RepositoryException ex) {
                    log.error("Error preparing to publish all documents: {}", ex);
                }
                return new ExtendedFolderWorkflowPlugin.ConfirmDialog(this,
                        new StringResourceModel("depublish-all-title", ExtendedFolderWorkflowPlugin.this, null),
                        new StringResourceModel("depublish-all-text", ExtendedFolderWorkflowPlugin.this, null),
                        new StringResourceModel("depublish-all-subtext", ExtendedFolderWorkflowPlugin.this, null),
                        new PropertyModel(ExtendedFolderWorkflowPlugin.this, "name"),
                        documents, query);
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                Session session = ((UserSession) getSession()).getJcrSession();
                WorkflowManager wfMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                for (String uuid : documents) {
                    try {
                        Node document = session.getNodeByIdentifier(uuid);
                        String path = document.getPath();
                        if (document.getDepth() > 0) {
                            Node handle = document.getParent();
                            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                                for (NodeIterator requestIter = handle.getNodes(HippoNodeType.NT_REQUEST); requestIter.hasNext(); ) {
                                    Node request = requestIter.nextNode();
                                    if (request != null) {
                                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, request);
                                        if (workflow instanceof FullRequestWorkflow) {
                                            ((FullRequestWorkflow) workflow).cancelRequest();
                                            log.info("removed request(s) from document "+document.getPath()+" ("+uuid+")");
                                        }
                                    }
                                }
                            }
                        }
                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, document);
                        if (workflow instanceof FullReviewedActionsWorkflow) {
                            ((FullReviewedActionsWorkflow) workflow).depublish();
                            ++processed;
                            log.info("depublished document "+path+" ("+uuid+")");
                        }
                    } catch (MappingException ex) {
                        log.warn("Depublication of {} failed: {}", uuid, ex);
                    } catch (WorkflowException ex) {
                        log.warn("Depublication of {} failed: {}", uuid, ex);
                    } catch (RemoteException ex) {
                        log.warn("Depublication of {} failed: {}", uuid, ex);
                    } catch (RepositoryException ex) {
                        log.warn("Depublication of {} failed: {}", uuid, ex);
                    }
                    session.refresh(true);
                }
            }
        });
    }

    public class ConfirmDialog extends WorkflowAction.WorkflowDialog {
        private IModel title;

        private Label affectedComponent;

        public ConfirmDialog(WorkflowAction action, IModel dialogTitle, IModel dialogText, IModel dialogSubText, IModel folderName, Set<String> documents, Query query) {
            action.super();
            this.title = dialogTitle;

            try {
                Node folder = (ExtendedFolderWorkflowPlugin.this.getDefaultModel() instanceof WorkflowDescriptorModel ? ((WorkflowDescriptorModel)ExtendedFolderWorkflowPlugin.this.getDefaultModel()).getNode() : null);
                if (query != null && folder != null) {
                    query.bindValue("basefolder", folder.getSession().getValueFactory().createValue(folder.getPath()));
                    QueryResult result = query.execute();
                    for (NodeIterator documentIter = result.getNodes(); documentIter.hasNext();) {
                        Node document = documentIter.nextNode();
                        if (document != null) {
                            if (document.isNodeType("mix:referenceable") && document.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                                documents.add(document.getIdentifier());
                            }
                        }
                    }
                } else {
                    error("Error preparing to (de)publish all documents");
                }
            } catch(RepositoryException ex) {
                log.error("Error preparing to (de)publish all documents", ex);
                error("Error preparing to (de)publish all documents");
            }

            Label textComponent = new Label("text");
            textComponent.setDefaultModel(dialogText);
            add(textComponent);
            
            add(new Label("counttext", dialogSubText));
            
            Label countComponent = new Label("count");
            countComponent.setDefaultModel(new Model<String>(Integer.toString(documents.size())));
            add(countComponent);
            
            Label locationComponent = new Label("location");
            locationComponent.setDefaultModel(new Model<String>((String) folderName.getObject()));
            add(locationComponent);

            affectedComponent = new Label("affected");
            affectedComponent.setVisible(false);
            add(affectedComponent);
        }

        @Override
        public IModel getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }

        @Override
        protected void handleSubmit() {
            setOkVisible(false);
            setCancelLabel(new StringResourceModel("done-label", ConfirmDialog.this, null));
            onOk();
            affectedComponent.setDefaultModel(new Model<String>(Integer.toString(processed)));
            affectedComponent.setVisible(true);
            AjaxRequestTarget.get().addComponent(this);
       }
    }
}
