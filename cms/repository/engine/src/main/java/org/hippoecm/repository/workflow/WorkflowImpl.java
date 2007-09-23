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
package org.hippoecm.repository.workflow;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.servicing.ServiceImpl;

public abstract class WorkflowImpl extends ServiceImpl implements Workflow
{
    protected WorkflowContext context;
    public WorkflowImpl() throws RemoteException {
    }
    final void setWorkflowContext(WorkflowContext context) {
        this.context = context;
    }
    final protected WorkflowContext getWorkflowContext() {
        return context;
    }

    /* These are used to overcome shortcomings in the mapping layer
     * implementation at this time.
     */
    public void pre() throws RepositoryException {
        // FIXME: workaround for current mapping issues
    }
    public void post() throws RepositoryException {
        // FIXME: workaround for current mapping issues
    }
}
