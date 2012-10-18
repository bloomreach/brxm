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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Documents in the repository can be retrieved as plain-old Java objects (POJOs) using a OCM mapping.  The document manager is
 * a service which allows for a sub-node structure to be mapped to a Java object.  The document manager is obtained from a
 * {@link HippoWorkspace} instance.
 */
public interface DocumentManager {

    /**
     * Document Managers are associated with an authenticated session.
     * @return the session with which this workflow manager object is associated
     * @throws RepositoryException a generic error while communicating with the repository
     */
    public Session getSession() throws RepositoryException;

    /**
     * Obtains the (plain-old) java object representation of the document in the specified category and which can be identified
     * using some abstract identification.
     * @param category the category from which to obtain a specific implementation
     * @param identifier some reference to a document
     * @return the representation of the document in the repository which is at least of the Document class
     * @throws RepositoryException a generic error while communicating with the repository
     */
    public Document getDocument(String category, String identifier) throws RepositoryException;
}
