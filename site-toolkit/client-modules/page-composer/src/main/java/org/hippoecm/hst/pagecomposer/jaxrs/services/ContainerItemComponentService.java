/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.ITEM_NOT_FOUND;

public class ContainerItemComponentService {

    private static final Logger log = LoggerFactory.getLogger(ContainerItemComponentService.class);

    private final PageComposerContextService pageComposerContextService;
    private final ContainerItemHelper containerItemHelper;

    public ContainerItemComponentService(PageComposerContextService pageComposerContextService, ContainerItemHelper containerItemHelper) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerItemHelper = containerItemHelper;
    }

    /**
     * Locks the container item identified by containerItemId with the given time stamp. Will throw a {@link
     * ClientException} if the item cannot be found, is not a container item or is already locked.
     *
     * @param containerItemId the identifier of the container item to be locked
     * @param versionStamp    timestamp used for the lock
     * @param session         a user bound session
     * @throws ClientException
     * @throws RepositoryException
     */
    public void lock(String containerItemId, long versionStamp, Session session) throws RepositoryException, ClientException {
        try {
            final Node containerItem = pageComposerContextService.getRequestConfigNodeById(containerItemId, NODETYPE_HST_CONTAINERITEMCOMPONENT, session);
            new HstComponentParameters(containerItem, containerItemHelper).lock(versionStamp);
            log.info("Component locked successfully.");
        } catch (ItemNotFoundException e) {
            throw new ClientException("container item with id " + containerItemId + " not found", ITEM_NOT_FOUND);
        }
    }

}
