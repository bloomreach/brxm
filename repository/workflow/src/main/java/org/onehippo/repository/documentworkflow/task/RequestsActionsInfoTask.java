/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow.task;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.Request;
import org.onehippo.repository.documentworkflow.WorkflowRequest;

/**
 * Custom workflow task for providing supported actions info for the Request nodes of a Document handle.
 */
public class RequestsActionsInfoTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        HashMap<String, Map<String, Boolean>> requestActionsMap = new HashMap<>();

        DocumentHandle dh = getDocumentHandle();
        dh.getInfo().put("requests", (Serializable)Collections.unmodifiableMap(requestActionsMap));
        for (Request req : dh.getRequests().values()) {
            Map actions = new HashMap<>();
            if (req.isWorkflowRequest()) {
                WorkflowRequest request = (WorkflowRequest)req;
                actions.put("cancelRequest", Boolean.valueOf(dh.getUser().equals(request.getOwner())));
                if (dh.isGranted(request, "hippo:editor")) {
                    actions.put("acceptRequest", Boolean.TRUE);
                    actions.put("rejectRequest", Boolean.TRUE);
                    // TODO: current 7.8 implementation allows editors to cancel requests for other users as well, but that seems odd
                    if (request.getOwner() == null) {
                        actions.put("cancelRequest", Boolean.TRUE);
                    }
                }
            }
            else if (req.isScheduledRequest()) {
                if (dh.isGranted(req, "hippo:editor")) {
                    actions.put("cancelRequest", Boolean.TRUE);
                }
            }
            if (!actions.isEmpty()) {
                // Also register these actions on handle workflow level
                // Note: this assumes *only* Boolean.TRUE actions are registered!
                dh.getActions().putAll(actions);
                requestActionsMap.put(req.getIdentity(), Collections.unmodifiableMap(actions));
            }
        }
        return null;
    }
}
