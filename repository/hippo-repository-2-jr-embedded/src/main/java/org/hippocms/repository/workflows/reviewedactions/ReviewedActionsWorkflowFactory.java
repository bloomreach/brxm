/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.workflows.reviewedactions;

import org.hippocms.repository.model.CurrentUsernameSource;
import org.hippocms.repository.model.Document;
import org.hippocms.repository.model.Scheduler;
import org.hippocms.repository.model.Workflow;
import org.hippocms.repository.model.WorkflowFactory;

public class ReviewedActionsWorkflowFactory implements WorkflowFactory {
    private CurrentUsernameSource currentUsernameSource;
    private Scheduler scheduler;

    public ReviewedActionsWorkflowFactory() {
        super();
    }

    public Workflow create(Document document) {
        ReviewedActionsWorkflow result = new ReviewedActionsWorkflow(document);
        result.setCurrentUsernameSource(currentUsernameSource);
        result.setScheduler(scheduler);
        return result;
    }

    public void setCurrentUsernameSource(CurrentUsernameSource currentUsernameSource) {
        this.currentUsernameSource = currentUsernameSource;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
