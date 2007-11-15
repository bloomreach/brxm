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
package org.hippoecm.repository.sample;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;

public class SampleWorkflowRenderer extends GenericWorkflowRenderer
{
  public SampleWorkflowRenderer(WorkflowManager manager, WorkflowDescriptor descriptor) {
    super(manager, descriptor);
  }
  @Override
public void invoke() throws WorkflowException, RepositoryException, RemoteException {
    SampleWorkflow myworkflow = (SampleWorkflow) workflowManager.getWorkflow(workflowDescriptor);
    if(myworkflow != null)
      myworkflow.renameAuthor("Jan Smit");
    else
      throw new WorkflowException("workflow no longer available: "+workflowDescriptor);
  }
}
