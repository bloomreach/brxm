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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.InvalidNodeTypeException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ItemNotFoundException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class ContainerComponentServiceImpl implements ContainerComponentService {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentServiceImpl.class);

    private PageComposerContextService pageComposerContextService;
    private ContainerHelper containerHelper;

    class ContainerItemImpl implements ContainerItem {

        private final Node containerItem;
        private final long timeStamp;

        public ContainerItemImpl(Node containerItem, long timeStamp) {
            this.containerItem = containerItem;
            this.timeStamp = timeStamp;
        }

        @Override
        public Node getContainerItem() {
            return containerItem;
        }

        @Override
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    public ContainerComponentServiceImpl(PageComposerContextService pageComposerContextService, ContainerHelper containerHelper) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerHelper = containerHelper;
    }

    @Override
    public ContainerItem createContainerItem(final Session session, final String itemUUID, final long versionStamp) throws RepositoryException, ClientException {
        final Node containerItem = getContainerItem(session, itemUUID);
        final Node containerNode = getContainer();

        lockContainer(containerNode, versionStamp);

        // now we have the containerItem that contains 'how' to create the new containerItem and we have the
        // containerNode. Find a correct newName and create a new node.
        final String newItemNodeName = findNewName(containerItem.getName(), containerNode);

        JcrUtils.copy(session, containerItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);
        final Node newItem = containerNode.getNode(newItemNodeName);

        HstConfigurationUtils.persistChanges(session);

        final long newVersionStamp = getVersionStamp(containerNode);
        return new ContainerItemImpl(newItem, newVersionStamp);
    }

    private long getVersionStamp(final Node node) throws RepositoryException {
        final long versionStamp;
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
            versionStamp = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
        } else {
            versionStamp = 0;
        }
        return versionStamp;
    }

    private void lockContainer(final Node containerNode, final long versionStamp) throws ClientException, RepositoryException {
        try {
            // the acquireLock also checks all ancestors whether they are not locked by someone else
            containerHelper.acquireLock(containerNode, versionStamp);
        } catch (ClientException e) {
            log.info("Exception while trying to lock '" + containerNode.getPath() + "': ", e);
            throw e;
        }
    }

    private Node getContainer() throws ClientException, RepositoryException {
        final Node containerNode = pageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        if (containerNode == null) {
            log.warn("Exception during creating new container item : Could not find container node to add item to.");
            throw new ItemNotFoundException("Could not find container node to add item to");
        }
        return containerNode;
    }

    private Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException, ClientException {
        final Node containerItem = session.getNodeByIdentifier(itemUUID);

        if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            log.warn("The container component where the item should be created in is not of the correct type. Cannot create item '{}'", itemUUID);
            throw new InvalidNodeTypeException("The container component where the item should be created in is not of the correct type", itemUUID);
        }
        return containerItem;
    }

    private static String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }
}
