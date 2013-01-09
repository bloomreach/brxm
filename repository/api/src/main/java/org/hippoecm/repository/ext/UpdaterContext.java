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
package org.hippoecm.repository.ext;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

/**
 * This Context is passed to the implementors of an {@link UpdaterModule} during initialization.
 * The UpdaterModule implementation must then use this context to populate the necessary information
 * for registration.  See the description of the methods in the UpdateContext on which methods must
 * and may be called.
 */
public interface UpdaterContext {

    /**
     * The registerName method MUST be called EXACTLY ONCE by every class
     * that implements the UpdaterModule abstract base class.
     * @param name the name of this module being registered
     */
    public void registerName(String name);

    /**
     * The registerBefore method MAY be called zero, once or multiple times.
     * @param name the name of the module to be registered before
     */
    public void registerBefore(String name);

    /**
     * The registerAfter method MAY be called zero, once or multiple times.
     * @param name the name of the module to be registered after
     */
    public void registerAfter(String name);

    /**
     * The registerStartTag method MUST be called AT LEAST ONCE.
     * @param name the begin goals from which to start an update, this tag is
     * removed when the updater has run.
     */
    public void registerStartTag(String name);
    
    /**
     * The registerExpectTag may be called zero, once or multiple times.
     * @param name the begin goal that must be present, but will not be removed.
     */
    public void registerExpectTag(String name);

    /**
     * The registerEndTag method MUST be called EXACTLY ONCE.
     * @param name the end goal reached after updating
     */
    public void registerEndTag(String name);

    /**
     * Register an item visitor that is called to perform the update.
     * @param visitor
     */
    public void registerVisitor(ItemVisitor visitor);

    /**
     * During an update, this is the save method to get the node-types for
     * a certain primary type, as a replacement for Node.getPrimaryNodeType()
     * and Node.getMixinNodeTypes()
     * @param session
     * @param type 
     * @return node type definition
     * @throws RepositoryException  in case of a generic error in the interaction with the repository
     */
    public NodeType getNewType(Session session, String type) throws RepositoryException;

    /**
     * During an update, the setName call may be used to rename an item safely.
     * @param item the item to be renamed
     * @param name the new name of the item
     * @throws javax.jcr.RepositoryException  in case of a generic error in the interaction with the repository
     */
    public void setName(Item item, String name) throws RepositoryException;

    /**
     * During an update, the setPrimaryNodeType call may be used to apply a
     * different primary node type.  Both old and new node-type definitions
     * are valid until just before changes are committed.
     * @param node the node for which to change the primary node type
     * @param name the new primary node type name
     * @throws javax.jcr.RepositoryException  in case of a generic error in the interaction with the repository
     */
    public void setPrimaryNodeType(Node node, String name) throws RepositoryException;

    /**
     * During an update, this is a convenient call to get all the node type
     * definitions, both primary and mixin node types of a certain node.
     * @param node the node for which to obtain the defined node types
     * @return an array of all node type definitions, the primary node type
     * will be the first in the array
     * @throws javax.jcr.RepositoryException in case of a generic error in the interaction with the repository
     */
    public NodeType[] getNodeTypes(Node node) throws RepositoryException;

    /**
     * Convenience call to retrieve whether the current set value of a property
     * is a multi value.
     * @param property the property to inspect
     * @return true if the set value is a multi-value
     * @throws RepositoryException  in case of a generic error in the interaction with the repository
     */
    public boolean isMultiple(Property property) throws RepositoryException;

    /**
     * Returns the workspace that may be used during the registration of the module, but should normally not be used during the
     * evaluation by visitors.  The returned workspace is not associated with a session and can only be used to query
     * the name-space registry.
     * @return a workspace that may only be used to access the name-space manager
     * @throws RepositoryException in case of a generic error in the interaction with the repository
     */
    public Workspace getWorkspace() throws RepositoryException;
}
