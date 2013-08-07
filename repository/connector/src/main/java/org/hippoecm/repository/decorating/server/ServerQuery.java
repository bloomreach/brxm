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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.server.ServerQueryResult;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.decorating.remote.RemoteQuery;

public class ServerQuery extends org.apache.jackrabbit.rmi.server.ServerQuery implements RemoteQuery {

    private HippoQuery query;

    protected ServerQuery(HippoQuery query, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(query, factory);
        this.query = query;
    }

    public String storeAsNode(String absPath, String type) throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException, UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        Node node = query.storeAsNode(absPath, type);
        return node.getPath();
    }

    public String[] getArguments() throws RepositoryException {
        return query.getArguments();
    }

    public int getArgumentCount() throws RepositoryException {
        return query.getArgumentCount();
    }

    public RemoteQueryResult execute(Map<String,String> arguments) throws RepositoryException, RemoteException {
        return new ServerQueryResult(query.execute(arguments), getFactory());
    }

    public void bindValue(String varName, Value value) throws RepositoryException, RemoteException {
        query.bindValue(varName, value);
    }

    public String[] getBindVariableNames() throws RepositoryException, RemoteException {
        return query.getBindVariableNames();
    }

    public void setLimit(long limit) throws RemoteException {
        query.setLimit(limit);
    }

    public void setOffset(long offset) throws RemoteException {
        query.setOffset(offset);
    }
}
