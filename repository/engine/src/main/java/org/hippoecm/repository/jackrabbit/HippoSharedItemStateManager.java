/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.Update;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOMAINSPATH;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;

public class HippoSharedItemStateManager extends SharedItemStateManager {

    private static final Logger log = LoggerFactory.getLogger(HippoSharedItemStateManager.class);

    public RepositoryImpl repository;

    private Name handleNodeName;
    private Name documentNodeName;
    private NodeId rootNodeId;
    private NodeTypeRegistry nodeTypeRegistry;

    private Collection<WeakReference<HandleListener>> handleListeners = new CopyOnWriteArrayList<>();

    private QFacetRuleStateManager qFacetRuleStateManager = new QFacetRuleStateManager();

    public HippoSharedItemStateManager(RepositoryImpl repository, PersistenceManager persistMgr, NodeId rootNodeId, NodeTypeRegistry ntReg, boolean usesReferences,
                                       ItemStateCacheFactory cacheFactory, ISMLocking locking, final NodeIdFactory nodeIdFactory) throws ItemStateException {
        super(persistMgr, rootNodeId, ntReg, usesReferences, cacheFactory, locking, nodeIdFactory);
        this.repository = repository;
        this.rootNodeId = rootNodeId;
        this.nodeTypeRegistry = ntReg;
        super.setEventChannel(new DocumentChangeNotifyingEventChannelDecorator());

    }

    @Override
    public void doPostInitialize() throws RepositoryException {
        final String SECURITY_CONFIG_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;

        if (getSystemSession().nodeExists(SECURITY_CONFIG_PATH)) {
            final Node securityFolderNode = getSystemSession().getNode(SECURITY_CONFIG_PATH);
            /* TODO: fix documentation at https://documentation.bloomreach.com/library/concepts/security/repository-authorization-and-permissions.html
                     the documentation says all SECURITY_CONFIG paths defaults are absolute (start *with* a slash),
                     but actually they must be root relative (*without* a prefixing slash)
             */
            final String domainsPath = "/" + securityFolderNode.getProperty(HIPPO_DOMAINSPATH).getString();
            if (getSystemSession().nodeExists(domainsPath)) {
                qFacetRuleStateManager.visit(getSystemSession().getNode(domainsPath));
            }
        }
    }

    public QFacetRuleStateManager getQFacetRuleStateManager() {
        return qFacetRuleStateManager;
    }

    @Override
    public void setEventChannel(final UpdateEventChannel upstream) {
        UpdateEventChannel eventChannel = new DocumentChangeNotifyingEventChannelDecorator(upstream);
        super.setEventChannel(eventChannel);
    }

    // FIXME: transactional update?

    @Override
    public void update(ChangeLog local, EventStateCollectionFactory factory) throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        super.update(local, factory);
    }

    @Override
    public void externalUpdate(ChangeLog external, EventStateCollection events) {
        super.externalUpdate(external, events);
        notifyDocumentListeners(external);
    }

    void notifyDocumentListeners(ChangeLog changeLog) {
        if (handleListeners.size() == 0) {
            return;
        }

        Name handleNodeName = getHandleName(repository);
        if (handleNodeName == null) {
            return;
        }
        Name documentNodeName = getDocumentName(repository);
        if (documentNodeName == null) {
            return;
        }

        try {
            Set<NodeState> handles = new HashSet<>();
            addHandles(changeLog.modifiedStates(), changeLog, handles);
            for (NodeState handleState : handles) {
                for (WeakReference<HandleListener> reference : handleListeners) {
                    final HandleListener listener = reference.get();
                    if (listener != null) {
                        listener.handleModified(handleState);
                    }
                }
            }
        } catch (ItemStateException e) {
            log.error("Could not broadcast handle changes", e);
        }
    }

    private void addHandles(final Iterable<ItemState> states, ChangeLog changes, final Set<NodeState> handles) throws ItemStateException {
        for (ItemState state : states) {
            try {
                final NodeState nodeState;
                if (state.isNode()) {
                    // REPO-492 node states originating from an external update are incomplete:
                    // they lack the node type name that is needed to identify them as handles
                    nodeState = (NodeState) getItemState(state.getId());
                } else {
                    if (changes.isModified(state.getParentId())) {
                        continue;
                    }
                    nodeState = (NodeState) getItemState(state.getParentId());
                }
                final Name nodeTypeName = nodeState.getNodeTypeName();
                if (nodeTypeName == null) {
                    log.warn("Node type name is null for " + nodeState.getId());
                    continue;
                }
                if (handleNodeName.equals(nodeTypeName)) {
                    handles.add(nodeState);
                } else {
                    final EffectiveNodeType ent = nodeTypeRegistry.getEffectiveNodeType(nodeTypeName);
                    if (ent.includesNodeType(documentNodeName)) {
                        final NodeState parentState = (NodeState) getItemState(nodeState.getParentId());
                        final Name parentNodeTypeName = parentState.getNodeTypeName();
                        if (parentNodeTypeName != null && handleNodeName.equals(parentNodeTypeName)) {
                            handles.add(parentState);
                        } else {
                            log.debug("Skipping {}, Id: '{}'", parentNodeTypeName, parentState.getNodeId());
                        }
                    }
                }
            } catch (NoSuchItemStateException e) {
                final String message = "Unable to add handles for modified state '" + state.getId() + "' because an item could not be found.";

                if (log.isDebugEnabled()) {
                    log.info(message, e);
                } else {
                    log.info(message + " (full stacktrace on debug level)");
                }
            } catch (NoSuchNodeTypeException e) {
                log.error("Could not find node type", e);
            }
        }
    }


    @Override
    public void addListener(final ItemStateListener listener) {
        super.addListener(listener);
        if (listener instanceof HandleListener) {
            handleListeners.add(new WeakReference<>((HandleListener) listener));
        }
    }

    @Override
    public void removeListener(final ItemStateListener listener) {
        if (listener instanceof HandleListener) {
            final WeakReference<HandleListener> reference = getHandleListenerReference(listener);
            if (reference != null) {
                handleListeners.remove(reference);
            }
        }
        super.removeListener(listener);
    }

    private WeakReference<HandleListener> getHandleListenerReference(final ItemStateListener listener) {
        for (WeakReference<HandleListener> reference : handleListeners) {
            final HandleListener handleListener = reference.get();
            if (handleListener == listener) {
                return reference;
            }
            if (handleListener == null) {
                handleListeners.remove(reference);
            }
        }
        return null;
    }

    private Name getHandleName(final RepositoryImpl repository) {
        if (handleNodeName == null) {
            try {
                final String hippoUri = repository.getNamespaceRegistry().getURI("hippo");
                handleNodeName = NameFactoryImpl.getInstance().create(hippoUri, "handle");
            } catch (NamespaceException e) {
                log.warn("hippo prefix not yet available");
            }
        }
        return handleNodeName;
    }

    private Name getDocumentName(final RepositoryImpl repository) {
        if (documentNodeName == null) {
            try {
                final String hippoUri = repository.getNamespaceRegistry().getURI("hippo");
                documentNodeName = NameFactoryImpl.getInstance().create(hippoUri, "document");
            } catch (NamespaceException e) {
                log.warn("hippo prefix not yet available");
            }
        }
        return documentNodeName;
    }

    @Override
    public boolean hasItemState(final ItemId id) {
        if (id.denotesNode()) {
            if (id instanceof HippoNodeId) {
                return false;
            }
        } else {
            PropertyId propertyId = (PropertyId) id;
            if (propertyId.getParentId() instanceof HippoNodeId) {
                return false;
            }
        }
        return super.hasItemState(id);
    }

    private class DocumentChangeNotifyingEventChannelDecorator implements UpdateEventChannel {
        private final UpdateEventChannel upstream;

        public DocumentChangeNotifyingEventChannelDecorator() {
            this(null);
        }

        public DocumentChangeNotifyingEventChannelDecorator(final UpdateEventChannel upstream) {
            this.upstream = upstream;
        }

        @Override
        public void updateCreated(final Update update) throws ClusterException {
            if (upstream != null) {
                upstream.updateCreated(update);
            }
        }

        @Override
        public void updatePrepared(final Update update) throws ClusterException {
            if (upstream != null) {
                upstream.updatePrepared(update);
            }
        }

        @Override
        public void updateCommitted(final Update update, final String path) {
            if (upstream != null) {
                upstream.updateCommitted(update, path);
            }
            try {
                notifyDocumentListeners(update.getChanges());
            } catch (Throwable t) {
                log.error("Exception thrown when notifying handle listeners", t);
            }
        }

        @Override
        public void updateCancelled(final Update update) {
            if (upstream != null) {
                upstream.updateCancelled(update);
            }
        }

        @Override
        public void setListener(final UpdateEventListener listener) {
            if (upstream != null) {
                upstream.setListener(listener);
            }
        }
    }


    @Override
    public void stateModified(final ItemState modified) {
        super.stateModified(modified);
        // TODO
    }

    @Override
    public void stateDiscarded(final ItemState discarded) {
        super.stateDiscarded(discarded);
        // TODO
    }

    @Override
    public void stateCreated(ItemState created) {
        super.stateCreated(created);


        if (rootNodeId == null) {
            // HSISM is being constructed still.
            return;
        }

        if (getNamePathResolver() == null) {
            // HSISM is being constructed still.
            return;
        }

        if (!(created instanceof  NodeState)) {
            return;
        }

        final NodeState createNodeState = (NodeState) created;
        try {
            if ((createNodeState).getNodeTypeName().equals(getNamePathResolver().getQName(NT_FACETRULE))) {
                final String path = getPath(created);
                final Node node = getSystemSession().getNode(path);
                qFacetRuleStateManager.visit(node);
                return;
            }

        } catch (RepositoryException | ItemStateException e) {
            log.error("Exception while processing created state.", e);
        }

        try {
            final String newPath = getPath(created);
            qFacetRuleStateManager.processNewPath(newPath, created);


        } catch (ItemStateException | NamespaceException | PathNotFoundException e) {
            log.error("Exception while processing created state.", e);
        }
    }

    @Override
    public void stateDestroyed(final ItemState destroyed) {
        super.stateDestroyed(destroyed);

        if (rootNodeId == null) {
            // HSISM is being constructed still.
            return;
        }

        if (getNamePathResolver() == null) {
            // HSISM is being constructed still.
            return;
        }

        if (destroyed instanceof NodeState) {
            final String destroyedId = destroyed.getId().toString();

            qFacetRuleStateManager.processDestroyedId(destroyedId);

        }
    }

    private String getPath(final ItemState created)
            throws ItemStateException, NamespaceException, PathNotFoundException {
        final StringBuilder pathBuilder = new StringBuilder();
        populatePath(pathBuilder, (NodeState) created);
        return pathBuilder.toString();
    }

    private void populatePath(final StringBuilder pathBuilder, final NodeState nodeState)
            throws ItemStateException, NamespaceException, PathNotFoundException {

        if (nodeState.getId().equals(rootNodeId)) {
            return;
        }
        // item got its parent (concurrently) removed and is 'free floating'
        if (nodeState.getParentId() == null) {
            throw new PathNotFoundException("Cannot construct path since parent is null");
        }
        final NodeState parentState = (NodeState) getItemState(nodeState.getParentId());

        final ChildNodeEntry childNodeEntry = parentState.getChildNodeEntry(nodeState.getNodeId());
        if (childNodeEntry == null) {
            // child has already been removed again (by other session)
            throw new PathNotFoundException("Cannot construct path since node has been removed already");
        }
        final Name name = childNodeEntry.getName();

        final String jcrName = getNamePathResolver().getJCRName(name);

        pathBuilder.insert(0, jcrName).insert(0, "/");

        populatePath(pathBuilder, parentState);
    }


}
