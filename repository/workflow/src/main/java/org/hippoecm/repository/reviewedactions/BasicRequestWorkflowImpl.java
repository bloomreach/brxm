/*
 *  Copyright 2008 Hippo.
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
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

@PersistenceCapable
public class BasicRequestWorkflowImpl extends WorkflowImpl implements BasicRequestWorkflow {

    @Persistent(column=".")
    protected PublicationRequest request;

    public BasicRequestWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
        if(request.getOwner() != null) {
            if(request.getOwner().equals(getWorkflowContext().getUserIdentity())) {
                info.put("cancelRequest", true);
            } else {
                info.put("cancelRequest", false);
            }
        }
        return info;
    }

    public void cancelRequest() throws WorkflowException, RepositoryException {
        request = null;
    }
}
