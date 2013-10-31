/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

public interface HstNode {

    /**
     * @return name of the hst node
     */
    String getName();

    /**
     * @return the value provider for this {@link HstNode}
     */
    ValueProvider getValueProvider();

    /**
     * @param relPath a path that does not start with a slash, for example 'foo' or 'foo/bar'.
     * @return the descendant node at <code>relPath</code> or <code>null</code> if it does not exist
     * @throws IllegalArgumentException if <code>relPath</code> is not a valid relPath
     */
    HstNode getNode(String relPath) throws IllegalArgumentException;

    /**
     * @return List<{@link HstNode}> of all the child nodes
     */
    List<HstNode> getNodes();

    /**
     * @return List<{@link HstNode}> of all the child nodes with {@link #getNodeTypeName()} equals to <code>nodeTypeName</code>
     */
    List<HstNode> getNodes(String nodeTypeName);

    /**
     * @return the node type of the backing provider
     */
    String getNodeTypeName();

    /**
     * @return the parent of this <code>{@link HstNode}</code> or <code>null</code> when there is no parent.
     */
    HstNode getParent();
    
    /**
     * Adds or replaces a child HST node with the given name. If an HstNode with the given name already exits, it is
     * replaced with the given HST node. Since the HstNode's are used for the HST config model that does not support
     * same name siblings this is not a problem.
     * @param name the name for the new HST node.
     * @param hstNode the HST node to add or replace.
     */
    void addNode(String name, HstNode hstNode);
    
    /**
     * removes child node with <code>name</code> and does nothing if not present
     * @param name
     */
    void removeNode(String name);
    
    /**
     * sets the new valueProvider
     * @param valueProvider
     */
    void setJCRValueProvider(JCRValueProvider valueProvider);

    /**
     * marks the HstNode as stale: The JCRValueProvider is out-of-date
     * @deprecated  since 7.9.0 : use {@link #markStaleByPropertyEvent()} or {@link #markStaleByNodeEvent(boolean)}
     */
    void markStale();

    /**
     * marks the HstNode as stale due to property event: The JCRValueProvider is out-of-date.
     */
    void markStaleByPropertyEvent();

    /**
     * marks the HstNode as stale due to node event: The JCRValueProvider might be out-of-date and/or the child nodes. If the
     * children's order matter, use childOrderedReload = true
     * @param childOrderedReload when the children's order is not of any meaning, orderAgnostic = true : This can result in
     *                      much more efficient reload
     */
    void markStaleByNodeEvent(boolean childOrderedReload);
    
    /**
     * @return <code>true</code> when this HstNode is stale
     */
    boolean isStale();

    /**
     * updates all the HstNode's that need reloading.
     * @param session
     */
    void update(Session session) throws RepositoryException;

}