/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.Set;
import java.util.SortedMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A lightweight and immutable representation of the DocumentType definitions.
 * If the model is still 'current' its version property can be compared against the current version provided by the {@link org.onehippo.cms7.services.contenttype.ContentTypeService}.
 */
public interface DocumentTypes {

    /**
     * @return The EffectiveNodeTypes used by this DocumentTypes instance.
     */
    EffectiveNodeTypes getEffectiveNodeTypes();

    /**
     * @return The immutable instance version which is automatically incremented for every new (changed) DocumentTypes instance created by the ContentTypeService
     */
    long version();

    /**
     * @param name Qualified Name for a DocumentType (see JCR-2.0 3.2.5.2)
     * @return The immutable DocumentType definition
     */
    DocumentType getType(String name);

    /**
     * @return The immutable map of DocumentTypes grouped and sorted by their namespace prefix as key and their elements ordered (but not sorted) by their name
     */
    SortedMap<String, Set<DocumentType>> getTypesByPrefix();

    /**
     * Returns the effective DocumentType representation for a specific Node
     * @param node The Node for which to retrieve the DocumentType representation
     * @return the effective DocumentType representation for a specific Node
     * @throws javax.jcr.RepositoryException if a repository error occurs
     */
    DocumentType getDocumentTypeForNode(Node node) throws RepositoryException;

    /**
     * Returns the effective DocumentType representation for an existing Node identified by its uuid
     * <p>
     * The existence and allowed read access to the Node is first checked through the provided Session.
     * </p>
     *
     * @param session An active repository Session
     * @param uuid An existing Node uuid
     * @return the effective DocumentType representation for an existing Node identified by its uuid
     * @throws javax.jcr.ItemNotFoundException if node doesn't exist or is not accessible
     * @throws RepositoryException if another error occurs
     */
    DocumentType getDocumentTypeForNodeByUuid(Session session, String uuid) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the effective DocumentType representation for an existing Node identified by its absolute path
     * <p>
     * The existence and allowed read access to the Node is first checked through the provided Session.
     * </p>
     * @param session An active repository Session
     * @param path The absolute path of an existing Node
     * @return the effective DocumentType representation for an existing Node identified by its absolute path
     * @throws javax.jcr.PathNotFoundException if node doesn't exist or is not accessible
     * @throws RepositoryException if a repository error occurs
     */
    DocumentType getDocumentTypeForNodeByPath(Session session, String path) throws PathNotFoundException, RepositoryException;
}
