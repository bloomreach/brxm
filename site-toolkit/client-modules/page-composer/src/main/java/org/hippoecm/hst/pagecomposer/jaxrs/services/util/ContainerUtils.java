/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.util;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class ContainerUtils {

    private static final Logger log = LoggerFactory.getLogger(ContainerUtils.class);

    public static Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException {

        try {
            final Node containerItem = session.getNodeByIdentifier(itemUUID);

            if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                log.info("The container component '{}' does not have the correct type. ", itemUUID);
                throw new ClientException(String.format("The container item '%s' does not have the correct type",
                        itemUUID), ClientError.INVALID_NODE_TYPE);
            }
            return containerItem;
        } catch (ItemNotFoundException e) {
            log.info("Cannot find container item '{}'.", itemUUID);
            throw new ClientException(String.format("Cannot find container item '%s'",
                    itemUUID), ClientError.INVALID_UUID);
        }
    }

    public static String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }

}
