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
package org.hippocms.repository.jr.servicing;

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Workspace;

import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteNode;

public class ServerServicingAdapterFactory extends ServerAdapterFactory
  implements RemoteServicingAdapterFactory
{
  public ServerServicingAdapterFactory() {
  }
  public RemoteSession getRemoteSession(Session session) throws RemoteException {
    if(session instanceof ServicingSessionImpl)
      return new ServerServicingSession((ServicingSessionImpl)session, this);
    else
      return super.getRemoteSession(session);
  }
  public RemoteWorkspace getRemoteWorkspace(Workspace workspace) throws RemoteException {
    if(workspace instanceof ServicingWorkspaceImpl)
      return new ServerServicingWorkspace((ServicingWorkspaceImpl)workspace, this);
    else
      return super.getRemoteWorkspace(workspace);
  }
  public RemoteNode getRemoteNode(Node node) throws RemoteException {
    if(node instanceof ServicingNodeImpl)
      return new ServerServicingNode((ServicingNodeImpl)node, this);
    else
      return super.getRemoteNode(node);
  }
  public RemoteServicesManager getRemoteServicesManager(ServicesManager servicesManager) throws RemoteException {
    return new ServerServicesManager(servicesManager, this);
  }
}
