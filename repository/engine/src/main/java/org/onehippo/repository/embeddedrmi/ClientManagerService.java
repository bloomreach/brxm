/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.embeddedrmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.ManagerService;

@Deprecated
public class ClientManagerService implements ManagerService {
    Session session;
    WorkflowManager workflowManager = null;
    HierarchyResolver hierarchyResolver = null;
    RemoteManagerService remote = null;

    public ClientManagerService(RemoteManagerService remote, Session session) {
        this.remote = remote;
        this.session = session;
    }
    
    public ClientManagerService(String url, Session session) throws RemoteException {
        try {
            remote = (RemoteManagerService)Naming.lookup(url);
            this.session = session;
        } catch (MalformedURLException e) {
            throw new RemoteException("Malformed URL: " + url, e);
        } catch (NotBoundException e) {
            throw new RemoteException("No target found: " + url, e);
        } catch (ClassCastException e) {
            throw new RemoteException("Unknown target: " + url, e);
        }
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            if (workflowManager == null) {
                try {
                    workflowManager = new ClientWorkflowManager(session, remote.getWorkflowManager((String)session.getAttribute("sessionName")));
                } catch (RemoteException ex) {
                    throw new RepositoryException("connection failure", ex);
                }
            }
            return workflowManager;
        } catch (LoginException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            throw ex;
        }
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        if (hierarchyResolver == null) {
            hierarchyResolver = new HierarchyResolverImpl();
        }
        return hierarchyResolver;
    }

    @Override
    public void close() {
        session = null;
        workflowManager = null;
        hierarchyResolver = null;
    }
}
