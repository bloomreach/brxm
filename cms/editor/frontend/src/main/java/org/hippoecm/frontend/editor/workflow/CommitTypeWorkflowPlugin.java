/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitTypeWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CommitTypeWorkflowPlugin.class);
    private MarkupContainer commitAction;

    public CommitTypeWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(commitAction = new WorkflowAction("commit", new StringResourceModel("commit", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected String execute(Workflow wf) {
                try {
                    EditmodelWorkflow workflow = (EditmodelWorkflow) wf;
                    if (workflow != null) {
                        workflow.commit();
                    }
                } catch (WorkflowException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RemoteException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RepositoryException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                }
                return null;
            }
        });

        try {
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("commit") && info.get("commit") instanceof Boolean && !((Boolean) info.get("commit")).booleanValue()) {
                    commitAction.setVisible(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
