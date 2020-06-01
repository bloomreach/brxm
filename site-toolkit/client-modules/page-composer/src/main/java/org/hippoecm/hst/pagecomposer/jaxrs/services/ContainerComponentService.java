/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public interface ContainerComponentService {
    interface ContainerItem {
        Node getContainerItem();
        long getTimeStamp();
    }

    ContainerItem createContainerItem(Session session, String itemUUID, long versionStamp) throws ClientException, RepositoryException;

    /**
     * Update order of items inside the container.
     *
     * @param session
     * @param container
     * @throws ClientException
     * @throws RepositoryException
     */
    void updateContainer(final Session session, final ContainerRepresentation container) throws ClientException, RepositoryException;

    /**
     * Delete a container item identified by the given <code>itemUUID</code>.
     * @param session
     * @param itemUUID
     * @param versionStamp
     * @throws ClientException
     * @throws RepositoryException
     */
    void deleteContainerItem(Session session, String itemUUID, long versionStamp) throws ClientException, RepositoryException;
}
