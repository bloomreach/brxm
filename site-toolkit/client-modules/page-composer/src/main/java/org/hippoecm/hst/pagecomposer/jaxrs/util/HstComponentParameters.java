/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;

public class HstComponentParameters extends AbstractHstComponentParameters {

    private ContainerItemHelper containerItemHelper;

    public HstComponentParameters(final Node node, final ContainerItemHelper containerItemHelper) throws RepositoryException {

        super(node);
        this.containerItemHelper = containerItemHelper;
    }

    public void save(long versionStamp) throws RepositoryException, IllegalStateException {
        setNodeChanges();
        lock(versionStamp);
        if (RequestContextProvider.get() == null) {
            node.getSession().save();
        } else {
            HstConfigurationUtils.persistChanges(node.getSession());
        }
    }

    public void lock(long versionStamp) throws RepositoryException, IllegalStateException {
        if (RequestContextProvider.get() != null) {
            if (!node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new IllegalStateException("Node to be saved must be of type '" + HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT + "' but " +
                        "was of type '" + node.getPrimaryNodeType().getName() + "'. Skip save");
            }
            containerItemHelper.acquireLock(node, versionStamp);
        }
    }

    public void unlock() throws RepositoryException {
        if (RequestContextProvider.get() != null) {
            if (!node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new IllegalStateException("Node to be saved must be of type '" + HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT + "' but " +
                        "was of type '" + node.getPrimaryNodeType().getName() + "'. Skip save");
            }
            containerItemHelper.releaseLock(node);
        }
    }

}
