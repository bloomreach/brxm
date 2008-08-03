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

import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteSessionInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteQueryInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.spi.rmi.remote.RemoteSubscription;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.ChildInfoImpl;
import org.apache.jackrabbit.spi.commons.NodeInfoImpl;
import org.apache.jackrabbit.spi.commons.PropertyInfoImpl;
import org.apache.jackrabbit.spi.commons.LockInfoImpl;
import org.apache.jackrabbit.spi.commons.SerializableBatch;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.util.IteratorHelper;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.RemoteObject;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.jackrabbit.rmi.remote.RemoteSession;


/**
 * <code>ServerRepositoryService</code> implements a remote repository service
 * based on a SPI {@link org.apache.jackrabbit.spi.RepositoryService}.
 */
public class ServerRepositoryService extends ServerObject implements RemoteRepositoryService {

    /**
     * The default iterator buffer size.
     */
    private static final int DEFAULT_BUFFER_SIZE = 100;

    /**
     * The underlying repository service to remote.
     */
    private RepositoryService service;

    RemoteSession jcrSession;

    /**
     * The id factory.
     */
    private final IdFactory idFactory = IdFactoryImpl.getInstance();

    /**
     * Maps remote stubs to {@link ServerSessionInfo}s.
     */
    private final Map activeSessionInfos = Collections.synchronizedMap(new HashMap());

    /**
     * Creates a new server repository service.
     *
     * @param service repository service to remote.
     */
    public ServerRepositoryService(RepositoryService service)
            throws RemoteException {
        this.service = service;
    }

    public ServerRepositoryService() throws RemoteException {
    }
    
    public void setRepositoryService(RepositoryService service) {
        this.service = service;
    }

    public void setRemoteSession(RemoteSession newSession) {
        jcrSession = newSession;
    }

    public RemoteSession getRemoteSession() {
        return jcrSession;
    }

    public boolean exists(RemoteSessionInfo sessionInfo, ItemId id)
            throws RepositoryException, RemoteException {
        return service.exists(getSessionInfo(sessionInfo), id);
    }

    public NodeId getRootId(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        return service.getRootId(getSessionInfo(sessionInfo));
    }

    /**
     * {@inheritDoc}
     */
    public Map getRepositoryDescriptors() throws RepositoryException, RemoteException {
        try {
            Map descriptors = service.getRepositoryDescriptors();
            if (descriptors instanceof Serializable) {
                return descriptors;
            } else {
                return new HashMap(descriptors);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteSessionInfo obtain(Credentials credentials,
                                    String workspaceName)
            throws RepositoryException, RemoteException {
        try {
            return createServerSessionInfo(service.obtain(credentials, workspaceName));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteSessionInfo obtain(RemoteSessionInfo sessionInfo,
                                    String workspaceName)
            throws RepositoryException, RemoteException {

        try {
            return createServerSessionInfo(
                    service.obtain(getSessionInfo(sessionInfo), workspaceName));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteSessionInfo impersonate(RemoteSessionInfo sessionInfo,
                                         Credentials credentials)
            throws RepositoryException, RemoteException {
        try {
            return createServerSessionInfo(
                    service.impersonate(getSessionInfo(sessionInfo), credentials));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        try {
            service.dispose(getSessionInfo(sessionInfo));
            activeSessionInfos.remove(RemoteObject.toStub(sessionInfo));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getWorkspaceNames(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        try {
            return service.getWorkspaceNames(getSessionInfo(sessionInfo));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(RemoteSessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions) throws RepositoryException, RemoteException {
        try {
            return service.isGranted(getSessionInfo(sessionInfo), itemId, actions);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QNodeDefinition getNodeDefinition(RemoteSessionInfo sessionInfo,
                                             NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            QNodeDefinition nDef = service.getNodeDefinition(
                    getSessionInfo(sessionInfo), nodeId);
            if (nDef instanceof Serializable) {
                return nDef;
            } else {
                return new QNodeDefinitionImpl(nDef);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QPropertyDefinition getPropertyDefinition(
            RemoteSessionInfo sessionInfo, PropertyId propertyId)
            throws RepositoryException, RemoteException {
        try {
            QPropertyDefinition pDef = service.getPropertyDefinition(
                    getSessionInfo(sessionInfo), propertyId);
            if (pDef instanceof Serializable) {
                return pDef;
            } else {
                return new QPropertyDefinitionImpl(pDef);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeInfo getNodeInfo(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            NodeInfo nInfo = service.getNodeInfo(getSessionInfo(sessionInfo), nodeId);
            return NodeInfoImpl.createSerializableNodeInfo(nInfo, idFactory);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator getItemInfos(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            Iterator it = service.getItemInfos(getSessionInfo(sessionInfo), nodeId);
            if (it instanceof RemoteIterator) {
                return (RemoteIterator) it;
            } else {
                List serializables = new ArrayList();
                while (it.hasNext()) {
                    ItemInfo info = (ItemInfo) it.next();
                    if (info instanceof Serializable) {
                        serializables.add(info);
                    } else {
                        if (info.denotesNode()) {
                            serializables.add(NodeInfoImpl.createSerializableNodeInfo((NodeInfo) info, idFactory));
                        } else {
                            serializables.add(PropertyInfoImpl.createSerializablePropertyInfo((PropertyInfo) info, idFactory));
                        }
                    }
                }
                return new ServerIterator(serializables.iterator(), DEFAULT_BUFFER_SIZE);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator getChildInfos(RemoteSessionInfo sessionInfo,
                                  NodeId parentId) throws RepositoryException, RemoteException {
        try {
            Iterator childInfos = service.getChildInfos(getSessionInfo(sessionInfo), parentId);
            return new ServerIterator(new IteratorHelper(childInfos) {
                public Object next() {
                    ChildInfo cInfo = (ChildInfo) super.next();
                    if (cInfo instanceof Serializable) {
                        return cInfo;
                    } else {
                        return new ChildInfoImpl(cInfo.getName(),
                                cInfo.getUniqueID(), cInfo.getIndex());
                    }
                }
            }, DEFAULT_BUFFER_SIZE);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyInfo getPropertyInfo(RemoteSessionInfo sessionInfo,
                                        PropertyId propertyId)
            throws RepositoryException, RemoteException {
        try {
            PropertyInfo propInfo = service.getPropertyInfo(getSessionInfo(sessionInfo), propertyId);
            return PropertyInfoImpl.createSerializablePropertyInfo(propInfo, idFactory);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void submit(RemoteSessionInfo sessionInfo, SerializableBatch batch)
            throws RepositoryException, RemoteException {
        try {
            Batch local = service.createBatch(
                    getSessionInfo(sessionInfo), batch.getSaveTarget());
            batch.replay(local);
            service.submit(local);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void importXml(RemoteSessionInfo sessionInfo,
                          NodeId parentId,
                          InputStream xmlStream,
                          int uuidBehaviour) throws RepositoryException, RemoteException {
        try {
            service.importXml(getSessionInfo(sessionInfo),
                    parentId, xmlStream, uuidBehaviour);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(RemoteSessionInfo sessionInfo,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     Name destName) throws RepositoryException, RemoteException {
        try {
            service.move(getSessionInfo(sessionInfo),
                    srcNodeId, destParentNodeId, destName);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(RemoteSessionInfo sessionInfo,
                     String srcWorkspaceName,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     Name destName) throws RepositoryException, RemoteException {
        try {
            service.copy(getSessionInfo(sessionInfo), srcWorkspaceName,
                    srcNodeId, destParentNodeId, destName);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(RemoteSessionInfo sessionInfo,
                       NodeId nodeId,
                       String srcWorkspaceName) throws RepositoryException, RemoteException {
        try {
            service.update(getSessionInfo(sessionInfo),
                    nodeId, srcWorkspaceName);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clone(RemoteSessionInfo sessionInfo,
                      String srcWorkspaceName,
                      NodeId srcNodeId,
                      NodeId destParentNodeId,
                      Name destName,
                      boolean removeExisting) throws RepositoryException, RemoteException {
        try {
            service.clone(getSessionInfo(sessionInfo), srcWorkspaceName,
                    srcNodeId, destParentNodeId, destName, removeExisting);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo getLockInfo(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            LockInfo lockInfo = service.getLockInfo(
                    getSessionInfo(sessionInfo), nodeId);
            if (lockInfo == null || lockInfo instanceof Serializable) {
                return lockInfo;
            } else {
                NodeId id = lockInfo.getNodeId();
                return new LockInfoImpl(lockInfo.getLockToken(),
                        lockInfo.getOwner(), lockInfo.isDeep(),
                        lockInfo.isSessionScoped(),
                        idFactory.createNodeId(id.getUniqueID(), id.getPath()));
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo lock(RemoteSessionInfo sessionInfo,
                         NodeId nodeId,
                         boolean deep,
                         boolean sessionScoped) throws RepositoryException, RemoteException {
        try {
            LockInfo lockInfo = service.lock(getSessionInfo(sessionInfo),
                    nodeId, deep, sessionScoped);
            if (lockInfo instanceof Serializable) {
                return lockInfo;
            } else {
                NodeId id = lockInfo.getNodeId();
                return new LockInfoImpl(lockInfo.getLockToken(),
                        lockInfo.getOwner(), lockInfo.isDeep(),
                        lockInfo.isSessionScoped(),
                        idFactory.createNodeId(id.getUniqueID(), id.getPath()));
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshLock(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            service.refreshLock(getSessionInfo(sessionInfo), nodeId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            service.unlock(getSessionInfo(sessionInfo), nodeId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkin(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            service.checkin(getSessionInfo(sessionInfo), nodeId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException {
        try {
            service.checkout(getSessionInfo(sessionInfo), nodeId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(RemoteSessionInfo sessionInfo,
                              NodeId versionHistoryId,
                              NodeId versionId) throws RepositoryException, RemoteException {
        try {
            service.removeVersion(getSessionInfo(sessionInfo),
                    versionHistoryId, versionId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restore(RemoteSessionInfo sessionInfo,
                        NodeId nodeId,
                        NodeId versionId,
                        boolean removeExisting) throws RepositoryException, RemoteException {
        try {
            service.restore(getSessionInfo(sessionInfo),
                    nodeId, versionId, removeExisting);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restore(RemoteSessionInfo sessionInfo,
                        NodeId[] versionIds,
                        boolean removeExisting) throws RepositoryException, RemoteException {
        try {
            service.restore(getSessionInfo(sessionInfo), versionIds, removeExisting);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator merge(RemoteSessionInfo sessionInfo,
                                NodeId nodeId,
                                String srcWorkspaceName,
                                boolean bestEffort) throws RepositoryException, RemoteException {
        try {
            Iterator it = service.merge(getSessionInfo(sessionInfo), nodeId, srcWorkspaceName, bestEffort);
            return new ServerIterator(it, DEFAULT_BUFFER_SIZE);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resolveMergeConflict(RemoteSessionInfo sessionInfo,
                                     NodeId nodeId,
                                     NodeId[] mergeFailedIds,
                                     NodeId[] predecessorIds)
            throws RepositoryException, RemoteException {
        try {
            service.resolveMergeConflict(getSessionInfo(sessionInfo),
                    nodeId, mergeFailedIds, predecessorIds);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addVersionLabel(RemoteSessionInfo sessionInfo,
                                NodeId versionHistoryId,
                                NodeId versionId,
                                Name label,
                                boolean moveLabel) throws RepositoryException, RemoteException {
        try {
            service.addVersionLabel(getSessionInfo(sessionInfo),
                    versionHistoryId, versionId, label, moveLabel);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersionLabel(RemoteSessionInfo sessionInfo,
                                   NodeId versionHistoryId,
                                   NodeId versionId,
                                   Name label) throws RepositoryException, RemoteException {
        try {
            service.removeVersionLabel(getSessionInfo(sessionInfo),
                    versionHistoryId, versionId, label);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        try {
            return service.getSupportedQueryLanguages(getSessionInfo(sessionInfo));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkQueryStatement(RemoteSessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map namespaces) throws RepositoryException, RemoteException {
        try {
            service.checkQueryStatement(getSessionInfo(sessionInfo),
                    statement, language, namespaces);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteQueryInfo executeQuery(RemoteSessionInfo sessionInfo,
                                        String statement,
                                        String language,
                                        Map namespaces)
            throws RepositoryException, RemoteException {
        try {
            QueryInfo qInfo = service.executeQuery(getSessionInfo(sessionInfo), statement, language, namespaces);
            return new ServerQueryInfo(qInfo, DEFAULT_BUFFER_SIZE, idFactory);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public EventFilter createEventFilter(RemoteSessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         Name[] nodeTypeName,
                                         boolean noLocal)
            throws RepositoryException, RemoteException {
        Set ntNames = null;
        if (nodeTypeName != null) {
            ntNames = new HashSet(Arrays.asList(nodeTypeName));
        }
        return new EventFilterImpl(eventTypes, absPath, isDeep, uuid, ntNames, noLocal);
    }

    /**
     * {@inheritDoc}
     */
    public RemoteSubscription createSubscription(RemoteSessionInfo sessionInfo,
                                                 EventFilter[] filters)
            throws RepositoryException, RemoteException {
        try {
            SessionInfo sInfo = getSessionInfo(sessionInfo);
            return new ServerSubscription(service, sInfo,
                    service.createSubscription(sInfo, filters), idFactory);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map getRegisteredNamespaces(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        try {
            Map namespaces = service.getRegisteredNamespaces(getSessionInfo(sessionInfo));
            if (namespaces instanceof Serializable) {
                return namespaces;
            } else {
                return new HashMap(namespaces);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(RemoteSessionInfo sessionInfo,
                                  String prefix) throws RepositoryException, RemoteException {
        try {
            return service.getNamespaceURI(getSessionInfo(sessionInfo), prefix);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix(RemoteSessionInfo sessionInfo,
                                     String uri) throws RepositoryException, RemoteException {
        try {
            return service.getNamespacePrefix(getSessionInfo(sessionInfo), uri);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerNamespace(RemoteSessionInfo sessionInfo,
                                  String prefix,
                                  String uri) throws RepositoryException, RemoteException {
        try {
            service.registerNamespace(getSessionInfo(sessionInfo), prefix, uri);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace(RemoteSessionInfo sessionInfo, String uri)
            throws RepositoryException, RemoteException {
        try {
            service.unregisterNamespace(getSessionInfo(sessionInfo), uri);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator getQNodeTypeDefinitions(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException {
        Iterator it = service.getQNodeTypeDefinitions(getSessionInfo(sessionInfo));
        return getQNodeTypeDefinitionIterator(it);
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator getQNodeTypeDefinitions(RemoteSessionInfo sessionInfo,
                                                 Name[] ntNames)
            throws RepositoryException, RemoteException {
        Iterator it = service.getQNodeTypeDefinitions(getSessionInfo(sessionInfo), ntNames);
        return getQNodeTypeDefinitionIterator(it);
    }

    //---------------------------< internal >-----------------------------------
    /**
     * Creates a server session info for the given <code>sessionInfo</code>.
     *
     * @param sessionInfo the session info.
     * @return a remote server session info.
     * @throws RemoteException if the rmi sub system fails to export the newly
     *                         created object.
     */
    private ServerSessionInfo createServerSessionInfo(SessionInfo sessionInfo)
            throws RemoteException {
        ServerSessionInfo ssInfo = new ServerSessionInfo(sessionInfo);
        activeSessionInfos.put(RemoteObject.toStub(ssInfo), ssInfo);
        return ssInfo;
    }

    /**
     * Retrieves the server session info for the given remote
     * <code>sInfo</code>.
     *
     * @param sInfo the remote session info.
     * @return the server session info.
     * @throws RepositoryException if server session info is not found for the
     *                             given remote session info.
     * @throws RemoteException     on rmi errors.
     */
    private SessionInfo getSessionInfo(RemoteSessionInfo sInfo)
            throws RepositoryException, RemoteException {
        Remote stub = RemoteObject.toStub(sInfo);
        ServerSessionInfo ssInfo = (ServerSessionInfo) activeSessionInfos.get(stub);
        if (ssInfo != null) {
            return ssInfo.getSessionInfo();
        } else {
            throw new RepositoryException("Unknown RemoteSessionInfo: " +
                    ((RemoteObject) sInfo).getRef());
        }
    }

    /**
     *
     * @param it
     * @return
     * @throws RemoteException
     */
    private RemoteIterator getQNodeTypeDefinitionIterator(Iterator it) throws RemoteException {
        List nts = new ArrayList();
        while (it.hasNext()) {
            QNodeTypeDefinition nt = (QNodeTypeDefinition) it.next();
            if (nt instanceof Serializable) {
                nts.add(nt);
            } else {
                nts.add(new QNodeTypeDefinitionImpl(nt));
            }
        }
        return new ServerIterator(nts.iterator(), DEFAULT_BUFFER_SIZE);
    }
}
