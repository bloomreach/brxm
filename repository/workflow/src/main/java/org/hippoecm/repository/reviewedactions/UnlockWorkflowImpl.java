/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.DocumentWorkflowImpl} instead.
 */
@Deprecated
public class UnlockWorkflowImpl extends AbstractReviewedActionsWorkflow implements UnlockWorkflow {

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws java.rmi.RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public UnlockWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        Map<String, Serializable> info = new HashMap<>(super.hints());
        if (info.containsKey("unlock")) {
            Map<String, Serializable> hints = new HashMap<>();
            hints.put("unlock", info.get("unlock"));
            return hints;
        }
        return Collections.emptyMap();
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.unlock();
    }

}
