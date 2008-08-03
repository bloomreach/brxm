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
package org.apache.jackrabbit.spi.rmi.server;

import org.apache.jackrabbit.spi.rmi.remote.RemoteSubscription;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.commons.EventImpl;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;

import javax.jcr.RepositoryException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>ServerSubscription</code>...
 */
class ServerSubscription extends ServerObject implements RemoteSubscription {

    /**
     * The repository service.
     */
    private final RepositoryService service;

    /**
     * The session info where this subscription belongs to.
     */
    private final SessionInfo sessionInfo;

    /**
     * The server side subscription.
     */
    private final Subscription subscription;

    /**
     * The id factory.
     */
    private final IdFactory idFactory;

    public ServerSubscription(RepositoryService service,
                              SessionInfo sessionInfo,
                              Subscription subscription,
                              IdFactory idFactory) throws RemoteException {
        this.service = service;
        this.sessionInfo = sessionInfo;
        this.subscription = subscription;
        this.idFactory = idFactory;
    }

    /**
     * {@inheritDoc}
     */
    public EventBundle[] getEvents(long timeout)
            throws RepositoryException, InterruptedException, RemoteException {
        try {
            EventBundle[] bundles = service.getEvents(subscription, timeout);
            EventBundle[] serBundles = new EventBundle[bundles.length];
            for (int i = 0; i < bundles.length; i++) {
                List events = new ArrayList();
                for (Iterator it = bundles[i].getEvents(); it.hasNext(); ) {
                    Event e = (Event) it.next();
                    ItemId id;
                    // make sure node ids are serializable
                    NodeId parentId = e.getParentId();
                    parentId = idFactory.createNodeId(
                            parentId.getUniqueID(), parentId.getPath());
                    if (e.getItemId().denotesNode()) {
                        NodeId nodeId = (NodeId) e.getItemId();
                        id = idFactory.createNodeId(nodeId.getUniqueID(), nodeId.getPath());
                    } else {
                        PropertyId propId = (PropertyId) e.getItemId();
                        id = idFactory.createPropertyId(parentId, propId.getName());
                    }
                    Event serEvent = new EventImpl(e.getType(),
                            e.getPath(), id, parentId,
                            e.getPrimaryNodeTypeName(),
                            e.getMixinTypeNames(), e.getUserID());
                    events.add(serEvent);
                }
                serBundles[i] = new EventBundleImpl(events, bundles[i].isLocal());
            }
            return serBundles;
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setFilters(EventFilter[] filters)
            throws RepositoryException, RemoteException {
        try {
            filters = createLocalEventFilters(service, sessionInfo, filters);
            service.updateEventFilters(subscription, filters);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws RepositoryException, RemoteException {
        try {
            service.dispose(subscription);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }
}
