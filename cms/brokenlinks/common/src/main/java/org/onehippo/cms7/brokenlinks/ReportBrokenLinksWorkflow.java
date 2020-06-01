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
package org.onehippo.cms7.brokenlinks;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.annotation.WorkflowAction;

public interface ReportBrokenLinksWorkflow extends Workflow {

    /**
     * Reports the existence of broken links in the document the workflow is executed on. If the collection of links is
     * empty, the broken links report of the document should be removed.
     *
     * @param brokenLinks
     * @throws WorkflowException
     * @throws RepositoryException
     * @throws RemoteException
     */
    @WorkflowAction(loggable=false)
    void reportBrokenLinks(Collection<Link> brokenLinks) throws WorkflowException, RepositoryException, RemoteException;

}
