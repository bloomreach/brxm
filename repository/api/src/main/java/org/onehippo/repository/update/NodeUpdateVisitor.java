/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Visitor for updating repository content.
 */
public interface NodeUpdateVisitor {

    /**
     * Allows initialization of this updater. Called before any other method is called.
     *
     * @param session a JCR {@link Session} with system credentials
     * @throws RepositoryException when thrown, the updater will not be run by the framework
     */
    void initialize(Session session) throws RepositoryException;

    /**
     * Update the given node.
     *
     * @param node  the {@link Node} to be updated
     * @return  <code>true</code> if the node was changed, <code>false</code> if not
     * @throws RepositoryException  if an exception occurred while updating the node
     */
    boolean doUpdate(Node node) throws RepositoryException;

    /**
     * Revert the given node. This method is intended to be the reverse of the {@link #doUpdate} method.
     * It allows update runs to be reverted in case a problem arises due to the update. The method should throw
     * an {@link UnsupportedOperationException} when it is not implemented.
     *
     * @param node  the node to be reverted.
     * @return  <code>true</code> if the node was changed, <code>false</code> if not
     * @throws RepositoryException  if an exception occurred while reverting the node
     * @throws UnsupportedOperationException if the method is not implemented
     */
    boolean undoUpdate(Node node) throws RepositoryException, UnsupportedOperationException;

    /**
     * Allows cleanup of resources held by this updater. Called after an updater run was completed.
     */
    void destroy();

}
