/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.replication.replicators;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.jackrabbit.SessionImpl;
import org.hippoecm.repository.replication.FatalReplicationException;
import org.hippoecm.repository.replication.RecoverableReplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Replicator that uses a remote JCR {@link Repository} to replicate changes over rmi.
 */
public class JCRReplicator extends AbstractReplicator {

    /** @exclude */

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(JCRReplicator.class);

    /** The remote repository address. */
    private String repositoryAddress;

    /** The user id for connecting to the remote repository. */
    private String username;

    /** The password for connecting to the remote repository. */
    private String password;

    /** The session to the remote repository. */
    private Session session = null;

    @Override
    protected String getId() {
        return "JCR[" + username + "@" + repositoryAddress + "]";
    }

    @Override
    protected void preDestroy() {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Override
    protected void connect() {
        obtainRemoteSession();
    }

    @Override
    protected void disconnect() {
        try {
            if (session != null && session.isLive()) {
                session.logout();
                log.debug("Disconnecting from Hippo Repository at '{}'.", getRepositoryAddress());
            }
        } catch (Exception e) {
            log.warn("Error while disconnecting from Hippo Repository: {}.", e);
        } finally {
            session = null;
        }
    }

    //------------------------------- Remote Session methods  -------------------------//

    /**
     * Get a {@link Session} instance to the remote {@link HippoRepository}.
     * <p>
     * <ul>
     * <li>TODO: There's a bug in the {@link HippoRepositoryFactory#getHippoRepository(String)} which causes the 
     *       factory to return the {@link LocalHippoRepository} if the rmi server of the specified remote 
     *       {@link Repository} is not available.
     * </ul>
     * 
     * @param workspaceName the {@link Workspace} name. Currently ignored.
     * @return the authenticated remote {@link Session}
     * @throws RecoverableReplicationException
     */
    protected Session obtainRemoteSession() {
        try {
            if (session == null || !session.isLive()) {
                log.debug("(Re)connecting to Hippo Repository at '{}'.", getRepositoryAddress());
                HippoRepository repository = HippoRepositoryFactory.getHippoRepository(getRepositoryAddress());
                session = repository.login(getUsername(), getPassword().toCharArray());
                log.debug("Connected to Hippo Repository at '{}'.", getRepositoryAddress());
            }
            session.refresh(false);
            return session;
        } catch (Exception e) {
            // any exception related to setting up the connection could/should be retried.
            session = null;
            throw new RecoverableReplicationException("Unable to obtain session to: " + getRepositoryAddress(), e);
        }
    }

    /**
     * Get the {@link Node} from the remote {@link Session} with the given {@link NodeId}.
     * <p>
     * <strong>ALERT</strong>: This current implementation of this method relies on a performance hack in Jackrabbit 1.5.
     * In future versions the getNodeByIdentifier from JCR 2.0 should be used.
     * 
     * @param session the {@link Session} to the remote {@link Repository}
     * @param id the {@link NodeId}
     * @return the remote {@link Node}
     * @throws RepositoryException 
     * @throws ItemNotFoundException 
     */
    private Node getRemoteNode(NodeId id) throws ItemNotFoundException, RepositoryException {
        return session.getNodeByUUID(id.toString());
    }

    @Override
    protected boolean hasRemoteNode(NodeId id) {
        try {
            session.getNodeByUUID(id.toString());
            return true;
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    protected String getRemoteJCRPath(NodeId id) {
        try {
            Node remoteNode = getRemoteNode(id);
            if (remoteNode == null) {
                return null;
            }
            return remoteNode.getPath();
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Override
    protected void saveRemoteChanges() {
        try {
            session.save();
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Unable to save remote changes", e);
        }
    }

    /**
     * Delete the node with the given {@link NodeId} from the remote {@link Repository}.
     * 
     * @param id the {@link NodeId}
     * @throws FatalReplicationException
     */
    @Override
    protected void removeRemoteNode(NodeId id) {
        try {
            Node remoteNode = getRemoteNode(id);
            remoteNode.remove();
            log.info("Replicator '{}': removed node: '{}'.", getId(), helper.getJCRPath(id));
        } catch (ItemNotFoundException e) {
            log.debug("Trying to delete node with id '{}', but node doesn't exists remotely", id);
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Error while deleting remote node", e);
        }
    }

    /**
     * Add the node with the given {@link NodeId} to the remote {@link Repository} and add 
     * all the properties of the node.
     * 
     * @param session the {@link Session} to the remote {@link Repository}
     * @param id the {@link NodeId}
     * @return the newly created {@link Node}
     * @throws FatalReplicationException
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void addRemoteNode(NodeId parentId, NodeId id) {
        NodeState state = helper.getNodeState(id);
        Node remoteParentNode = null;
        String parentPath = helper.getJCRPath(parentId);

        try {
            remoteParentNode = getRemoteNode(parentId);
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Unable to add node to remote repository with id: " + state.getId(), e);
        }

        if (remoteParentNode == null) {
            throw new FatalReplicationException(
                    "Parent node not found. Unable to add node to remote repository with id: " + state.getId());
        }

        try {
            Name name = helper.getNodeName(state);
            String nodeName = helper.getJCRName(name);
            String nodeType = helper.getJCRName(state.getNodeTypeName());

            Node remoteNode = createNode(remoteParentNode, nodeName, nodeType, id);

            if ("/".equals(parentPath)) {
                log.info("Replicator '{}': created node: '{}'.", getId(), "/" + nodeName);
            } else {
                log.info("Replicator '{}': created node: '{}'.", getId(), parentPath + "/" + nodeName);
            }
            
            // set mixins
            Set<Name> mixins = state.getMixinTypeNames();
            for (Name mixin : mixins) {
                setMixin(remoteNode, mixin);
            }

            // copy properties
            Iterator<Name> iter = state.getPropertyNames().iterator();
            while (iter.hasNext()) {
                setProperty(id, remoteNode, iter.next());
            }
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Unable to add node to remote repository with id: " + state.getId(), e);
        } catch (NoSuchItemStateException e) {
            throw new FatalReplicationException("Unable to add node to remote repository with id: " + state.getId(), e);
        } catch (ItemStateException e) {
            throw new FatalReplicationException("Unable to add node to remote repository with id: " + state.getId(), e);
        }
    }

    /**
     * Set and/or create all the properties from the {@link ItemState}s in the {@link Iterator} to
     * the remote node with the {@link NodeId}.
     * 
     * @param session the {@link Session} to the remote {@link Repository}
     * @param id the {@link NodeId}
     * @param iter the {@link Iterator} with the {@link ItemState}s
     * @throws FatalReplicationException
     */
    @Override
    protected void setRemoteProperties(NodeId id, Iterator<ItemState> iter) {
        Node remoteNode;
        try {
            remoteNode = getRemoteNode(id);
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Error while trying to find remote node for adding properties", e);
        }
        while (iter.hasNext()) {
            ItemState state = iter.next();
            if (state.isNode()) {
                log.error("The state list should only contain property states. Skipping: {}", state.getId());
                continue;
            }
            try {
                setProperty(id, remoteNode, ((PropertyState) state).getName());
            } catch (NoSuchItemStateException e) {
                throw new FatalReplicationException("Unable to add property to remote node with id: " + state.getId(),
                        e);
            } catch (ItemStateException e) {
                throw new FatalReplicationException("Unable to add property to remote node with id: " + state.getId(),
                        e);
            } catch (RepositoryException e) {
                throw new FatalReplicationException("Unable to add property to remote node with id: " + state.getId(),
                        e);
            }
        }
    }

    /**
     * Delete all the properties from the {@link ItemState}s in the {@link Iterator} from
     * the remote node with the {@link NodeId}.
     * 
     * @param session the {@link Session} to the remote {@link Repository}
     * @param id the {@link NodeId}
     * @param iter the {@link Iterator} with the {@link ItemState}s
     * @throws FatalReplicationException
     */
    @Override
    protected void removeRemoteProperties(NodeId id, Iterator<ItemState> iter) {
        Node remoteNode;
        try {
            remoteNode = getRemoteNode(id);
        } catch (ItemNotFoundException e) {
            log.debug("Trying to delete properties of node with id '{}', but node doesn't exists remotely", id);
            return;
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Error while trying to find remote node for adding properties", e);
        }
        while (iter.hasNext()) {
            ItemState state = iter.next();
            if (state.isNode()) {
                log.error("The state list should only contain property states. Skipping: {}", state.getId());
                continue;
            }
            try {
                deleteProperty(id, remoteNode, ((PropertyState) state).getName());
            } catch (RepositoryException e) {
                throw new FatalReplicationException("Unable to add property to remote node with id: " + state.getId(),
                        e);
            }
        }
    }

    /**
     * Set a mixin on the remote {@link Node}.
     * 
     * @param remoteNode the remote {@link Node}
     * @param mixinName the mixin {@link Name}
     * @throws RepositoryException
     */
    private void setMixin(Node remoteNode, Name mixinName) throws RepositoryException {
        remoteNode.addMixin(context.getNamePathResolver().getJCRName(mixinName));
    }

    /**
     * Set a property on the remote {@link Node}.
     * 
     * @param id the {@link NodeId} of the {@link Node}
     * @param remoteNode the remote {@link Node}
     * @param propName the property {@link Name}
     * @throws ItemStateException 
     * @throws NoSuchItemStateException 
     * @throws RepositoryException
     */
    private void setProperty(NodeId id, Node remoteNode, Name propName) throws NoSuchItemStateException,
            ItemStateException, RepositoryException {
        if (propertyIsVirtual(propName)) {
            return;
        }

        for(PropertyDefinition propDefinition : remoteNode.getPrimaryNodeType().getPropertyDefinitions())
            if(propDefinition.getName().equals(helper.getJCRName(propName)) && propDefinition.isProtected())
                return;
        for(NodeType mixinNodeType : remoteNode.getMixinNodeTypes())
            for(PropertyDefinition propDefinition : mixinNodeType.getPropertyDefinitions())
                if(propDefinition.getName().equals(helper.getJCRName(propName)) && propDefinition.isProtected())
                    return;

        PropertyState propState = helper.getPropertyState(id, propName);
       Value[] values = helper.getPropertyValues(propState, (SessionImpl)session); // FIXME: wrong session passed
        if (propState.isMultiValued()) {
            remoteNode.setProperty(context.getNamePathResolver().getJCRName(propName), values, propState.getType());
        } else {
            remoteNode.setProperty(context.getNamePathResolver().getJCRName(propName), values[0]);
        }
    }

    /**
     * Delete a property from the remote {@link Node}.
     * 
     * @param id the {@link NodeId} of the {@link Node}
     * @param remoteNode the remote {@link Node}
     * @param propName the property {@link Name}
     * @throws RepositoryException
     */
    private void deleteProperty(NodeId id, Node remoteNode, Name propName) throws RepositoryException {
        String relPath = context.getNamePathResolver().getJCRName(propName);
        if (remoteNode.hasProperty(relPath)) {
            Property prop = remoteNode.getProperty(relPath);
            if (!prop.getDefinition().isProtected()) {
                prop.remove();
            }
        }
    }

    /**
     * Create a child {@link Node} in the remote parent node with a specified {@link NodeId}
     * <p>
     * <strong>ALERT</strong>: The current implementation of this method (ab)uses
     * the {@link Session#importXML(String, InputStream, int)} method. It has to be seen if
     * this is robust enough and performs well enough.
     * 
     * @param remoteParent the remote parent {@link Node}
     * @param name the {@link Name} of the new {@link Node}
     * @param primaryType the primary type of the new {@link Node}
     * @param id the {@link NodeId} of the new {@link Node}
     * @return the newly created remote {@link Node}
     * @throws InvalidSerializedDataException
     * @throws IOException
     * @throws RepositoryException
     */
    private Node createNode(Node remoteParent, String name, String primaryType, NodeId id) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<sv:node sv:name=\"").append(name).append("\" ");
        sb.append("xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\">\n");
        sb.append("  <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n");
        sb.append("    <sv:value>").append(primaryType).append("</sv:value>\n");
        sb.append("  </sv:property>\n");
        sb.append("  <sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">\n");
        sb.append("    <sv:value>").append(id).append("</sv:value>\n");
        sb.append("  </sv:property>\n");
        sb.append("</sv:node>\n");

        try {
            InputStream in = new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes("UTF-8")));
            ((HippoSession) session).importDereferencedXML(remoteParent.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT,
                    ImportMergeBehavior.IMPORT_MERGE_DISABLE);
            return getRemoteNode(id);
        } catch (IOException e) {
            throw new FatalReplicationException("Unable to add remote node '" + name + "' of type '" + primaryType
                    + "'", e);
        } catch (RepositoryException e) {
            throw new FatalReplicationException("Unable to add remote node '" + name + "' of type '" + primaryType
                    + "'", e);
        }
    }

    //------------------------------- Getters and setters  -------------------------//

    public String getRepositoryAddress() {
        return repositoryAddress;
    }

    public void setRepositoryAddress(String repositoryAddress) {
        this.repositoryAddress = repositoryAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
