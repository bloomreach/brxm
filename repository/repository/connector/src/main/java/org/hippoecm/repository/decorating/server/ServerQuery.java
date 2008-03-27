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
package org.hippoecm.repository.decorating.server;

import java.util.Map;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.server.ServerQueryResult;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.decorating.remote.RemoteServicingAdapterFactory;
import org.hippoecm.repository.decorating.remote.RemoteQuery;

public class ServerQuery extends org.apache.jackrabbit.rmi.server.ServerQuery implements RemoteQuery {
    private HippoQuery query;

    public ServerQuery(HippoQuery query, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(query, factory);
        this.query = query;
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

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException, RemoteException {
        query.bindValue(varName, value);
    }
}
