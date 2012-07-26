/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.api;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * This interface is not yet part of the public API of the Hippo Repository.
 */
public interface HierarchyResolver {
    /**
     * 
     */

    /**
     * 
     */
    public final class Entry {
        /**
         * 
         */
        public Node node;
        /**
         * 
         */
        public String relPath;
    };

    /**
     * 
     * @param ancestor
     * @param path
     * @param isProperty
     * @param last
     * @return
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     */
    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException;

    /**
     * 
     * @param ancestor
     * @param path
     * @return
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     */
    public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException;

    /**
     * 
     * @param node
     * @param field
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public Property getProperty(Node node, String field) throws RepositoryException;

    /**
     * 
     * @param node
     * @param field
     * @param last
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public Property getProperty(Node node, String field, Entry last) throws RepositoryException;

    /**
     * 
     * @param node
     * @param field
     * @return
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     */
    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException;
}
