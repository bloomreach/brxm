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
package org.hippoecm.repository.decorating.remote;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;

public interface RemoteQuery extends org.apache.jackrabbit.rmi.remote.RemoteQuery, Remote, Serializable {
    public String[] getArguments() throws RepositoryException, RemoteException;
    public int getArgumentCount() throws RepositoryException, RemoteException;
    public RemoteQueryResult execute(Map<String,String> arguments) throws RepositoryException, RemoteException;
    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException, RemoteException;
}
