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

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.WorkflowPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowMappingException;

public class ReviewedActionsPlugin extends WorkflowPlugin {
    private static final long serialVersionUID = 1L;

    public ReviewedActionsPlugin(String id, JcrNodeModel model, WorkflowManager workflowManager, WorkflowDescriptor workflowDescriptor) {
        super(id, model, workflowManager, workflowDescriptor);
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
       try {
        Workflow workflow = getWorkflowManager().getWorkflow(getWorkflowDescriptor());
    } catch (WorkflowMappingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (RepositoryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    }

}
