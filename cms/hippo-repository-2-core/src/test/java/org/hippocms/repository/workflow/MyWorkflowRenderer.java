/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
package org.hippocms.repository.workflow;

import javax.jcr.RepositoryException;

import org.hippocms.repository.jr.servicing.WorkflowManager;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.workflow.WorkflowDescriptor;

public class MyWorkflowRenderer extends GenericWorkflowRenderer
{
  public MyWorkflowRenderer(WorkflowManager manager, WorkflowDescriptor descriptor) {
    super(manager, descriptor);
  }
  public void invoke() throws WorkflowException, RepositoryException {
    MyWorkflow myworkflow = (MyWorkflow) workflowManager.getWorkflow(workflowDescriptor);
    myworkflow.renameAuthor("Jan Smit");
  }
}
