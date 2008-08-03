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

import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.Event;

import javax.jcr.RepositoryException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <code>RemoteSubscription</code>...
 */
public interface RemoteSubscription extends Remote {

    /**
     * Retrieves the events that occurred since the last call to this method.
     *
     * @param timeout a timeout in milliseconds to wait at most for an event
     *                bundle.
     * @return an array of <code>EventBundle</code>s representing the external
     *         events that occurred.
     * @throws RepositoryException if an error occurs while retrieving the event
     *                             bundles.
     * @throws RemoteException     if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getEvents(SessionInfo, long, Subscription)
     */
    public EventBundle[] getEvents(long timeout)
            throws RepositoryException, InterruptedException, RemoteException;

    /**
     * Sets events filters on this subscription. When this method returns all
     * events that go through this subscription and have been generated after
     * this method call must be filtered using the passed <code>filters</code>.
     * <p/>
     * An implementation is required to accept at least event filter instances
     * created by {@link RepositoryService#createEventFilter}. Optionally an
     * implementation may also support event filters instanciated by the client
     * itself. An implementation may require special deployment in that case,
     * e.g. to make the event filter implementation class available to the
     * repository server.
     *
     * @param filters the filters that are applied to the events as they
     *                occurred on the repository. An event is included in an
     *                event bundle if it is {@link EventFilter#accept(Event,
     *                boolean)}  accept}ed by at least one of the supplied
     *                filters. If an empty array is passed none of the potential
     *                events are include in an event bundle. This allows a
     *                client to skip or ignore events for a certain period of
     *                time.
     * @throws RepositoryException if an error occurs while setting new filters.
     * @throws RemoteException if a communication error occurs.
     * @throws NullPointerException if <code>filters</code> is null.
     */
    public void setFilters(EventFilter[] filters)
            throws RepositoryException, RemoteException;

    /**
     * Indicates to this subscription that it will no longer be needed.
     *
     * @throws RepositoryException if an error occurs while this subscription is
     *                             disposed.
     * @throws RemoteException if a communication error occurs.
     */
    public void dispose() throws RepositoryException, RemoteException;
}
