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
package org.hippoecm.repository.decorating.client;

import java.util.Map;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.decorating.remote.RemoteQuery;

public class ClientQuery extends org.apache.jackrabbit.rmi.client.ClientQuery implements HippoQuery {
    private RemoteQuery remote;
    private Session session;

    public ClientQuery(Session session, RemoteQuery remote, LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.session = session;
        this.remote = remote;
    }

    public String[] getArguments() throws RepositoryException {
        try {
            return remote.getArguments();
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public int getArgumentCount() throws RepositoryException {
        try {
            return remote.getArgumentCount();
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        try {
            return getFactory().getQueryResult(session, remote.execute(arguments));
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        try {
            remote.bindValue(varName, value);
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void setLimit(long limit) throws RepositoryException {
        try {
            remote.setLimit(limit);
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void setOffset(long offset) throws RepositoryException {
        try {
            remote.setOffset(offset);
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
