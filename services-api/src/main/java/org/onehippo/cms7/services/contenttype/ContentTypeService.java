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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;

/**
 * ContentType Service which provides access to:
 * <ul>
 *     <li>A lightweight and immutable representation of the current and aggregated or effective JCR Repository NodeType definitions</li>
 *     <li>A lightweight and immutable representation of the current DocumentType definitions</li>
 *     <li>A effective DocumentType representation for an specific Node or a Node identified by its uuid or absolute path</li>
 * </ul>
 *
 */
@SingletonService
public interface ContentTypeService {

    /**
     * @return A lightweight and immutable representation of the current and aggregated or effective JCR Repository NodeType definitions
     * @throws RepositoryException if a repository error occurs
     */
    EffectiveNodeTypes getEffectiveNodeTypes() throws RepositoryException;

    /**
     * @return A lightweight and immutable representation of the current DocumentType definitions.
     * @throws RepositoryException if a repository error occurs
     */
    DocumentTypes getDocumentTypes() throws RepositoryException;
}
