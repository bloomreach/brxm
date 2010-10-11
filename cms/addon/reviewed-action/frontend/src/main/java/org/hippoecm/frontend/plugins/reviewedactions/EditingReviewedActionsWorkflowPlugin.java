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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.ActionDescription;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.feedback.YuiFeedbackPanel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingReviewedActionsWorkflowPlugin.class);

    static class FeedbackLogger extends Component {
        private static final long serialVersionUID = 1L;

        public FeedbackLogger() {
            super("id");
        }

        @Override
        protected void onRender(MarkupStream markupStream) {
        }

    }

    private Fragment feedbackContainer;
    private transient boolean closing = false;
    private boolean isValid = true;

    public EditingReviewedActionsWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final CompatibilityWorkflowPlugin plugin = this;
        final IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);

        add(new WorkflowAction("save", new StringResourceModel("save", this, null, "Save").getString(),
                new ResourceReference(EditingReviewedActionsWorkflowPlugin.class, "document-save-16.png")) {
            @Override
            protected String execute(Workflow wf) throws Exception {
                validate();
                if (!isValid()) {
                    return null;
                }

                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                new FeedbackLogger().info(new StringResourceModel("saved", EditingReviewedActionsWorkflowPlugin.this,
                        null, new Object[]{df.format(new Date())}).getString());
                showFeedback();

                UserSession session = (UserSession) Session.get();
                session.getJcrSession().refresh(false);

                // get new instance of the workflow, previous one may have invalidated itself
                EditingReviewedActionsWorkflowPlugin.this.getDefaultModel().detach();
                WorkflowDescriptor descriptor = (WorkflowDescriptor) (EditingReviewedActionsWorkflowPlugin.this
                        .getDefaultModel().getObject());
                session.getJcrSession().refresh(true);
                WorkflowManager manager = session.getWorkflowManager();
                workflow = (BasicReviewedActionsWorkflow) manager.getWorkflow(descriptor);

                /* Document draft = */
                workflow.obtainEditableInstance();
                return null;
            }
        });

        add(new WorkflowAction("done", new StringResourceModel("done", this, null, "Done").getString(),
                new ResourceReference(EditingReviewedActionsWorkflowPlugin.class, "document-saveclose-16.png")) {
            @Override
            public String execute(Workflow wf) throws Exception {
                validate();
                if (!isValid()) {
                    return null;
                }

                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                ((UserSession) Session.get()).getJcrSession().refresh(true);
                return null;
            }
        });

        Feedback fb = new Feedback();
        feedbackContainer = (Fragment) fb.getFragment("text");
        add(new Feedback());
    }


    protected void showFeedback() {
        YuiFeedbackPanel yfp = (YuiFeedbackPanel) feedbackContainer.get("feedback");
        yfp.render(AjaxRequestTarget.get());
    }

    void validate() throws ValidationException {
        isValid = true;
        List<IValidationService> validators = getPluginContext().getServices(
                getPluginConfig().getString(IValidationService.VALIDATE_ID), IValidationService.class);
        if (validators != null) {
            for (IValidationService validator : validators) {
                validator.validate();
                IValidationResult result = validator.getValidationResult();
                isValid = isValid && result.isValid();
            }
        }
    }

    boolean isValid() {
        return isValid;
    }

    class Feedback extends ActionDescription {
        private static final long serialVersionUID = 1L;

        public Feedback() {
            super("info");

            Fragment feedbackFragment = new Fragment("text", "feedback", EditingReviewedActionsWorkflowPlugin.this);
            feedbackFragment.add(new YuiFeedbackPanel("feedback", new IFeedbackMessageFilter() {
                private static final long serialVersionUID = 1L;

                public boolean accept(FeedbackMessage message) {
                    return FeedbackLogger.class.isInstance(message.getReporter());
                }
            }));
            add(feedbackFragment);
        }

        @Override
        protected void invoke() {
        }

    }
}
