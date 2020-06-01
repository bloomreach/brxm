/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

public interface ContainerComponentService {
    interface ContainerItem {
        Node getContainerItem();

        long getTimeStamp();
    }

    /**
     * Creates a container item by using the node with id itemUUID as a template and adds it as
     * last item to the container.
     *
     * @param session      session to use for repository interaction
     * @param itemUUID     id of item to use as template
     * @param versionStamp version stamp used for locking the container
     * @return newly created container item.
     * @throws RepositoryException in case reading/writing with session fails
     */
    ContainerItem createContainerItem(Session session, String itemUUID, long versionStamp) throws RepositoryException;

    /**
     * Creates a container item by using the node with id itemUUID as a template and adds it before the item
     * with id siblingItemUUID. If the sibling item is not a child item of the container then the newly
     * created container item will be appended as last.
     *
     * @param session         session to use for repository interaction
     * @param itemUUID        id of item to use as template
     * @param siblingItemUUID id of item where new item will be inserted
     * @param versionStamp    version stamp used for locking the container
     * @return newly created container item.
     * @throws RepositoryException in case reading/writing with session fails
     */
    ContainerItem createContainerItem(Session session, String itemUUID, String siblingItemUUID, long versionStamp) throws RepositoryException;

    /**
     * Update order of items inside the container.
     *
     * @param session   session to use for repository interaction
     * @param container container to update
     * @throws RepositoryException in case reading/writing with session fails
     */
    void updateContainer(final Session session, final ContainerRepresentation container) throws RepositoryException;

    /**
     * Delete a container item identified by the given <code>itemUUID</code>.
     *
     * @param session      session to use for repository interaction
     * @param itemUUID     id of container to delete
     * @param versionStamp version stamp used for locking the container
     * @throws RepositoryException in case reading/writing with session fails
     */
    void deleteContainerItem(Session session, String itemUUID, long versionStamp) throws RepositoryException;
}
