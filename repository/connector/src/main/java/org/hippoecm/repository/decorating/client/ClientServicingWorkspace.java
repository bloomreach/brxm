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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientWorkspace;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteServicingWorkspace;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;
import org.onehippo.repository.security.SecurityService;

public class ClientServicingWorkspace extends ClientWorkspace implements HippoWorkspace {

    private Session session;
    private RemoteServicingWorkspace remote;

    protected ClientServicingWorkspace(Session session, RemoteServicingWorkspace remote,
            LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.session = session;
        this.remote = remote;
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            RemoteWorkflowManager manager = remote.getWorkflowManager();
            return ((LocalServicingAdapterFactory) getFactory()).getWorkflowManager(session, manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        try {
            RemoteHierarchyResolver resolver = remote.getHierarchyResolver();
            return ((LocalServicingAdapterFactory) getFactory()).getHierarchyResolver(session, resolver);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public SecurityService getSecurityService() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
