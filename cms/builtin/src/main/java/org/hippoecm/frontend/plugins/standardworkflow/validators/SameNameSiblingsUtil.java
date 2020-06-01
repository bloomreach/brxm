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

package org.hippoecm.frontend.plugins.standardworkflow.validators;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public final class SameNameSiblingsUtil {

    private SameNameSiblingsUtil() {
        // intentionally blank
    }

    /**
     * Return true if <code>parentNode</code> contains a child having the same display name with the specified
     * <code>displayName</code>
     */
    public static boolean hasChildWithDisplayName(final Node parentNode, final String displayName) throws RepositoryException {
        final NodeIterator children = parentNode.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (child.isNodeType(HippoStdNodeType.NT_FOLDER) || child.isNodeType(HippoNodeType.NT_HANDLE)) {
                String childName = ((HippoNode) child).getDisplayName();
                if (StringUtils.equals(childName, displayName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
