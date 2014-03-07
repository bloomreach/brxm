/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class ContainerHelper extends AbstractHelper {

    @SuppressWarnings("unchecked")
    @Override
    public HstComponentConfiguration getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * if the <code>container</code> is already locked for the user <code>container.getSession()</code> this method does not do
     * anything. If there is no lock yet, a lock for the current session userID gets set on the container. If there is
     * already a lock by another user a ClientException is thrown,
     */
    public void acquireLock(final Node container, final long versionStamp) throws RepositoryException {
        if (!container.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
            throw new ClientException(String.format("Expected container of type '%s' but was of type '%s'",
                    HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, container.getPrimaryNodeType().getName()),
                    ClientError.INVALID_NODE_TYPE);
        }
        lockHelper.acquireLock(container, versionStamp);
    }
}
