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

import java.text.DateFormat;
import java.util.Date;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.addon.workflow.ActionDescription;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.feedback.YuiFeedbackPanel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.repository.api.Workflow;

public class EditingWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    static class FeedbackLogger extends Component {
        private static final long serialVersionUID = 1L;

        public FeedbackLogger() {
            super("id");
        }

        @Override
        protected void onRender() {
        }
    }

    private Fragment feedbackContainer;

    public EditingWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("save", new StringResourceModel("save", this, null, "Save"),
                new PackageResourceReference(EditingWorkflowPlugin.class, "img/document-save-16.png"), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService("service.edit", IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.save();

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                new FeedbackLogger().info(new StringResourceModel("saved", EditingWorkflowPlugin.this,
                        null, null, df.format(new Date())).getString());
                showFeedback();
                return null;
            }
        });

        add(new StdWorkflow("done", new StringResourceModel("done", this, null, "Done"),
                new PackageResourceReference(EditingWorkflowPlugin.class, "img/document-saveclose-16.png"), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            public String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService("service.edit", IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.done();
                return null;
            }
        });

        Feedback fb = new Feedback();
        feedbackContainer = (Fragment) fb.getFragment("text");
        add(fb);
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    protected void showFeedback() {
        YuiFeedbackPanel yfp = (YuiFeedbackPanel) feedbackContainer.get("feedback");
        yfp.render(RequestCycle.get().find(AjaxRequestTarget.class));
    }

    class Feedback extends ActionDescription {
        private static final long serialVersionUID = 1L;

        public Feedback() {
            super("info");

            Fragment feedbackFragment = new Fragment("text", "feedback", EditingWorkflowPlugin.this);
            feedbackFragment.add(new YuiFeedbackPanel("feedback", new IFeedbackMessageFilter() {
                private static final long serialVersionUID = 1L;

                public boolean accept(FeedbackMessage message) {
                    return FeedbackLogger.class.isInstance(message.getReporter());
                }
            }));
            add(feedbackFragment);
        }

        @Override
        public String getSubMenu() {
            return "info";
        }

        @Override
        protected void invoke() {
        }

    }
}
