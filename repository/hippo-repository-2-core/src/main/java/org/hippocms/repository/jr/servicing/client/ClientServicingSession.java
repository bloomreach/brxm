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
package org.hippocms.repository.jr.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.hippocms.repository.jr.servicing.ServicingSession;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingSession;

public class ClientServicingSession extends ClientSession implements ServicingSession, XAResource {
    private RemoteServicingSession remote;

    public ClientServicingSession(Repository repository, RemoteServicingSession remote,
            LocalServicingAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    public XAResource getXAResource() {
        return this;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            remote.commit(xid, onePhase);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            remote.end(xid, flags);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            remote.forget(xid);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return remote.getTransactionTimeout();
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        try {
            return remote.isSameRM(xares);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public int prepare(Xid xid) throws XAException {
        try {
            return remote.prepare(xid);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            return remote.recover(flag);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            remote.rollback(xid);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        try {
            return remote.setTransactionTimeout(seconds);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }

    public void start(Xid xid, int flags) throws XAException {
        try {
            remote.start(xid, flags);
        } catch (RemoteException ex) {
            throw new XAException("remote operation failed: " + ex.getMessage());
        }
    }
}
