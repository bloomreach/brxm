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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validaters;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.util.NodeIterable;

public abstract class AbstractLockValidator implements Validator {

    /**
     * returns the userID that contains the deep lock or <code>null</code> when no deep lock present
     */
    protected String getLockedDeepBy(final Node node, final String rootNodeType) throws RepositoryException {
        if (node == null) {
            return null;
        }

        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return lockedBy;
        }
        // no need to check higher than sitemap
        if (node.isNodeType(rootNodeType)) {
            return null;
        }
        return getLockedDeepBy(node.getParent(), rootNodeType);
    }

    protected boolean hasDescendantLock(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            if (!lockedBy.equals(node.getSession())) {
                return true;
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            boolean hasDescendantLock = hasDescendantLock(child);
            if (hasDescendantLock) {
                return true;
            }
        }
        return false;
    }


}
