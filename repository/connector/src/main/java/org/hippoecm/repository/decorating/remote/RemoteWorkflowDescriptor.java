/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating.remote;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;

public interface RemoteWorkflowDescriptor extends Remote {

    public String getDisplayName() throws RepositoryException, RemoteException;

    public String getAttribute(String name) throws RepositoryException, RemoteException;

    public Map<String,Serializable> hints() throws RepositoryException, RemoteException;

    public Workflow getWorkflow() throws RepositoryException, RemoteException;

    public String[] getInterfaces() throws ClassNotFoundException, RepositoryException, RemoteException;
}
