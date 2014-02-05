/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishAllShortcutPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(PublishAllShortcutPlugin.class);

    private final static String QUERY_LANGUAGE_PUBLISH = Query.SQL;
    private final static String QUERY_STATEMENT_PUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '/content/%' AND hippostd:state='unpublished'";
    private final static String QUERY_LANGUAGE_DEPUBLISH = Query.SQL;
    private final static String QUERY_STATEMENT_DEPUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '/content/%' AND hippostd:state='published'";
    private final static String WORKFLOW_CATEGORY = "default";

    private final static String MODE_PUBLISH = "publish";
    private final static String MODE_DEPUBLISH = "depublish";

    public PublishAllShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink<String> link = new AjaxLink<String>("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new PublishAllShortcutPlugin.Dialog(config));
            }
        };
        link.setModel(new StringResourceModel(config.getString("label.link"), this, null));
        add(link);
        Label label = new Label("label");
        label.setDefaultModel(new StringResourceModel(config.getString("label.link"), this, null));
        link.add(label);
    }

    public static class Dialog extends AbstractDialog {

        private static final long serialVersionUID = 1L;

        private Set<String> documents = new HashSet<String>();
        private String mode = MODE_PUBLISH;
        private IPluginConfig config;

        public Dialog(IPluginConfig config) {
            this.config = config;

            if (config.containsKey("mode")) {
                mode = config.getString("mode", MODE_PUBLISH);
            }

            if (mode.equals(MODE_PUBLISH)) {
                setOkLabel(new ResourceModel("button-publish"));
            } else if (mode.equals(MODE_DEPUBLISH)) {
                setOkLabel(new ResourceModel("button-depublish"));
            } else {
                setOkLabel(new ResourceModel("button-execute"));
            }
            
            try {
                Session session = UserSession.get().getJcrSession();
                QueryManager qMgr = session.getWorkspace().getQueryManager();
                Query query = null;
                if (mode.equals(MODE_PUBLISH)) {
                    query = qMgr.createQuery(QUERY_STATEMENT_PUBLISH, QUERY_LANGUAGE_PUBLISH);
                } else if (mode.equals(MODE_DEPUBLISH)) {
                    query = qMgr.createQuery(QUERY_STATEMENT_DEPUBLISH, QUERY_LANGUAGE_DEPUBLISH);
                }
                if (query != null) {
                    QueryResult result = query.execute();
                    for (NodeIterator documentIter = result.getNodes(); documentIter.hasNext();) {
                        Node document = documentIter.nextNode();
                        if (document != null) {
                            if (document.isNodeType("mix:referenceable") && document.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                                documents.add(document.getIdentifier());
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Error preparing to publish all documents: {}", ex);
            }

            Label countLabel = new Label("count");
            countLabel.setDefaultModel(new Model<String>(Integer.toString(documents.size())));
            add(countLabel);
        }

        public IModel getTitle() {
            return new StringResourceModel(config.getString("label.title"), this, null);
        }

        @Override
        public void onOk() {
            try {
                Session session = UserSession.get().getJcrSession();
                WorkflowManager wfMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                for (String uuid : documents) {
                    try {
                        Node document = session.getNodeByIdentifier(uuid);
                        if (document.getDepth() > 0) {
                            Node handle = document.getParent();
                            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                                for (NodeIterator requestIter = handle.getNodes(HippoNodeType.NT_REQUEST); requestIter.hasNext();) {
                                    Node request = requestIter.nextNode();
                                    if (request != null) {
                                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, request);
                                        if (workflow instanceof DocumentWorkflow) {
                                            ((DocumentWorkflow) workflow).cancelRequest(request.getIdentifier());
                                        }
                                    }
                                }
                            }
                        }
                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, document);
                        if (workflow instanceof DocumentWorkflow) {
                            if (mode.equals(MODE_PUBLISH)) {
                                ((DocumentWorkflow) workflow).publish();
                            } else if (mode.equals(MODE_DEPUBLISH)) {
                                ((DocumentWorkflow) workflow).depublish();
                            }
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
            } catch (RepositoryException ex) {
                log.error("Publication of all documents failed: {}", ex);
            }
        }

        @Override
        public IValueMap getProperties() {
            return SMALL;
        }
    }
}
