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
package org.hippoecm.repository.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;

public class ClientWorkflowManager extends ClientManager implements WorkflowManager {
  private Session session;
  private RemoteWorkflowManager remote;

  public ClientWorkflowManager(Session session, RemoteWorkflowManager remote, LocalServicingAdapterFactory factory) {
    super(factory);
    this.session = session;
    this.remote = remote;
  }

    public void save() throws RepositoryException {
    }

  public Session getSession() throws RepositoryException {
    return session;
  }

  public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        ClassLoader current = setContextClassLoader();
    try {
      return remote.getWorkflowDescriptor(category, item.getPath());
    } catch(RemoteException ex) {
      throw new RemoteRuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
    }
  }

  public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        ClassLoader current = setContextClassLoader();
    try {
      return remote.getWorkflow(category, item.getPath());
    } catch(RemoteException ex) {
      throw new RemoteRuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
    }
  }

  public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        ClassLoader current = setContextClassLoader();
    try {
      return remote.getWorkflow(descriptor);
    } catch(RemoteException ex) {
      throw new RemoteRuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
    }
  }
}
