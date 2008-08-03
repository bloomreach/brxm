/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.rmi.remote;

import org.apache.jackrabbit.spi.Name;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <code>RemoteQueryInfo</code>...
 */
public interface RemoteQueryInfo extends Remote {

    /**
     * @return an iterator over the serializable {@link org.apache.jackrabbit.spi.QueryResultRow}s
     * @see javax.jcr.query.QueryResult#getRows()
     */
    public RemoteIterator getRows() throws RemoteException;

    /**
     * @return an array of Name representing the column names of the query
     *         result.
     * @see javax.jcr.query.QueryResult#getColumnNames()
     */
    public Name[] getColumnNames() throws RemoteException;
}
