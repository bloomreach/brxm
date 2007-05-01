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

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteSession;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public interface RemoteServicingSession extends RemoteSession, Remote, Serializable {
    public void commit(Xid xid, boolean onePhase) throws RemoteException;
    public void end(Xid xid, int flags) throws RemoteException;
    public void forget(Xid xid) throws RemoteException;
    public int getTransactionTimeout() throws RemoteException;
    public boolean isSameRM(XAResource xares) throws RemoteException;
    public int prepare(Xid xid) throws RemoteException;
    public Xid[] recover(int flag) throws RemoteException;
    public void rollback(Xid xid) throws RemoteException;
    public boolean setTransactionTimeout(int seconds) throws RemoteException;
    public void start(Xid xid, int flags) throws RemoteException;
}
