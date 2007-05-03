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

import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.XASession;
import org.apache.jackrabbit.rmi.server.ServerSession;
import org.apache.jackrabbit.rmi.remote.RemoteSession;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.hippocms.repository.jr.servicing.remote.RemoteServicingSession;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingAdapterFactory;
import org.hippocms.repository.jr.servicing.ServicingSessionImpl;

public class ServerServicingSession extends ServerSession
  implements RemoteServicingSession
{
    private ServicingSessionImpl session;
    public ServerServicingSession(ServicingSessionImpl session, RemoteServicingAdapterFactory factory)
      throws RemoteException
    {
      super(session, factory);
      this.session = session;
    }

    public void commit(Xid xid, boolean onePhase) throws RemoteException {
      try {
        ((XASession)session).getXAResource().commit(xid, onePhase);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public void end(Xid xid, int flags) throws RemoteException {
      try {
        ((XASession)session).getXAResource().end(xid, flags);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public void forget(Xid xid) throws RemoteException {
      try {
        ((XASession)session).getXAResource().forget(xid);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public int getTransactionTimeout() throws RemoteException {
      try {
        return ((XASession)session).getXAResource().getTransactionTimeout();
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public boolean isSameRM(XAResource xares) throws RemoteException {
      try {
        return ((XASession)session).getXAResource().isSameRM(xares);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public int prepare(Xid xid) throws RemoteException {
      try {
        return ((XASession)session).getXAResource().prepare(xid);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public Xid[] recover(int flag) throws RemoteException {
      try {
        return ((XASession)session).getXAResource().recover(flag);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public void rollback(Xid xid) throws RemoteException {
      try {
        ((XASession)session).getXAResource().rollback(xid);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public boolean setTransactionTimeout(int seconds) throws RemoteException {
      try {
        return ((XASession)session).getXAResource().setTransactionTimeout(seconds);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
    public void start(Xid xid, int flags) throws RemoteException {
      try {
        ((XASession)session).getXAResource().start(xid, flags);
      } catch(XAException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }
}
