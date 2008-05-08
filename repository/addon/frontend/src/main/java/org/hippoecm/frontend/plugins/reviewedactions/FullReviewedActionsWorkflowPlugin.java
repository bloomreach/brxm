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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.ServiceTracker;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    public static final String EDITOR_ID = "workflow.editor";

    private ServiceTracker<IEditService> editor;

    @SuppressWarnings("unused")
    private String caption = "unknown document";
    private String stateSummary = "UNKNOWN";

    public FullReviewedActionsWorkflowPlugin() {

        editor = new ServiceTracker<IEditService>(IEditService.class);

        add(new Label("caption", new PropertyModel(this, "caption")));

        add(new Label("status", new PropertyModel(this, "stateSummary")));

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                List<IEditService> editors = editor.getServices();
                if (editors.size() > 0) {
                    editors.get(0).edit(new JcrNodeModel(docNode));
                }
            }
        });

        addWorkflowAction("requestPublication-dialog", "Request publication", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));
            }
        }, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestPublication();
            }
        });

        addWorkflowAction("requestDePublication-dialog", "Request unpublication", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new"));
            }
        }, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
            }
        });

        addWorkflowAction("requestDeletion-dialog", "Request delete", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));
            }
        }, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
            }
        });

        addWorkflowAction("publish-dialog", "Publish", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live"));

            }
        }, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
            }
        });

        addWorkflowAction("dePublish-dialog", "Unpublish", new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new"));
            }
        }, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
            }
        });

        addWorkflowAction("delete-dialog", "Unpublish and/or delete", new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
            }
        });
    }

    @Override
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);
        if (properties.get(EDITOR_ID) != null) {
            editor.open(context, properties.get(EDITOR_ID).getStrings().get(0));
        } else {
            log.warn("No editor ({}) specified for {}", EDITOR_ID, serviceId);
        }
    }

    @Override
    public void destroy() {
        editor.close();
        super.destroy();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            Node node = model.getNodeModel().getNode();
            caption = node.getName();

            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();)
                    node = iter.nextNode(); // FIXME: take the last one, the first should be good enough
            }
            if (node.hasProperty("hippostd:stateSummary"))
                stateSummary = node.getProperty("hippostd:stateSummary").getString();
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
        }
    }
}
