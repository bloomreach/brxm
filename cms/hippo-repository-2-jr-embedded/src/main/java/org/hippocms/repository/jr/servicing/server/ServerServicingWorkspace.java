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
package org.hippocms.repository.jr.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.Workspace;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

import org.hippocms.repository.jr.servicing.ServicesManager;
import org.hippocms.repository.jr.servicing.ServicingWorkspaceImpl;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingWorkspace;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingAdapterFactory;
import org.hippocms.repository.jr.servicing.remote.RemoteServicesManager;

public class ServerServicingWorkspace extends ServerWorkspace
  implements RemoteServicingWorkspace
{
  private ServicingWorkspaceImpl workspace;
  public ServerServicingWorkspace(ServicingWorkspaceImpl workspace, RemoteServicingAdapterFactory factory)
    throws RemoteException
  {
    super(workspace, factory);
    this.workspace = workspace;
  }
  public RemoteServicesManager getServicesManager()
    throws RemoteException, RepositoryException
  {
    try {
      ServicesManager servicesManager = workspace.getServicesManager();
      return ((RemoteServicingAdapterFactory)getFactory()).getRemoteServicesManager(servicesManager);
    } catch(RepositoryException ex) {
      throw getRepositoryException(ex);
    }
  }
}
