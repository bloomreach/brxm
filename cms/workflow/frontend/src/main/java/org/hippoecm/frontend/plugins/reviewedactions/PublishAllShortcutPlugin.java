/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Map;
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
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.ContextPayloadProvider;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowTransition;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.DocumentWorkflowAction.CANCEL_REQUEST;
import static org.hippoecm.repository.api.DocumentWorkflowAction.REJECT_REQUEST;

public class PublishAllShortcutPlugin extends RenderPlugin {

    private static Logger log = LoggerFactory.getLogger(PublishAllShortcutPlugin.class);

    private static final String QUERY_LANGUAGE_PUBLISH = Query.SQL;
    private static final String QUERY_STATEMENT_PUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '/content/%' AND hippostd:state='unpublished'";
    private static final String QUERY_LANGUAGE_DEPUBLISH = Query.SQL;
    private static final String QUERY_STATEMENT_DEPUBLISH = "SELECT * FROM hippostd:publishable WHERE jcr:path LIKE '/content/%' AND hippostd:state='published'";
    private static final String WORKFLOW_CATEGORY = "default";

    private final static String MODE_PUBLISH = "publish";
    private final static String MODE_DEPUBLISH = "depublish";

    public PublishAllShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink<String> link = new AjaxLink<String>("link") {
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

        private Set<String> handles = new HashSet<>();
        private String mode = MODE_PUBLISH;
        private IPluginConfig config;

        public Dialog(IPluginConfig config) {
            this.config = config;

            if (config.containsKey("mode")) {
                mode = config.getString("mode", MODE_PUBLISH);
            }

            switch (mode) {
                case MODE_PUBLISH:
                    setOkLabel(new ResourceModel("button-publish"));
                    break;
                case MODE_DEPUBLISH:
                    setOkLabel(new ResourceModel("button-depublish"));
                    break;
                default:
                    setOkLabel(new ResourceModel("button-execute"));
                    break;
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
                                handles.add(document.getParent().getIdentifier());
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Error preparing to publish all documents: {}", ex);
            }

            Label countLabel = new Label("count");
            countLabel.setDefaultModel(Model.of(Integer.toString(handles.size())));
            add(countLabel);
        }

        public IModel<String> getTitle() {
            return new StringResourceModel(config.getString("label.title"), this, null);
        }

        @Override
        public void onOk() {
            try {
                Session session = UserSession.get().getJcrSession();
                WorkflowManager wfMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                for (String uuid : handles) {
                    try {
                        Node handle = session.getNodeByIdentifier(uuid);
                        Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, handle);
                        if (!(workflow instanceof DocumentWorkflow)) {
                            continue;
                        }

                        DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflow;
                        Map<String, Map<String, Serializable>> requests = (Map<String, Map<String, Serializable>>)
                                documentWorkflow.hints(ContextPayloadProvider.get()).get("requests");
                        for (Map.Entry<String, Map<String, Serializable>> entry : requests.entrySet()) {
                            Map<String, Serializable> actions = entry.getValue();
                            if (Boolean.TRUE.equals(actions.get("cancelRequest"))) {
                                documentWorkflow.transition(getBuilder()
                                        .action(CANCEL_REQUEST)
                                        .requestIdentifier(entry.getKey())
                                        .build());
                                documentWorkflow.cancelRequest(entry.getKey());
                            } else if (Boolean.TRUE.equals(actions.get("rejectRequest"))) {
                                documentWorkflow.transition(getBuilder()
                                        .action(REJECT_REQUEST)
                                        .requestIdentifier(entry.getKey())
                                        .eventPayload("reason","bulk (de)publish")
                                        .build());
                            }
                        }

                        Map<String, Serializable> hints = documentWorkflow.hints(ContextPayloadProvider.get());
                        if (mode.equals(MODE_PUBLISH) && Boolean.TRUE.equals(hints.get("publish"))) {
                            ((DocumentWorkflow) workflow).publish();
                        } else if (mode.equals(MODE_DEPUBLISH) && Boolean.TRUE.equals(hints.get("depublish"))) {
                            ((DocumentWorkflow) workflow).depublish();
                        } else {
                            log.info("Unable to (de)publish {}", uuid);
                        }
                    } catch (WorkflowException | RemoteException | RepositoryException ex) {
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
            return DialogConstants.SMALL;
        }
    }

    private static WorkflowTransition.Builder getBuilder() {
        return new WorkflowTransition.Builder()
                .contextPayload(ContextPayloadProvider.get());
    }
}
