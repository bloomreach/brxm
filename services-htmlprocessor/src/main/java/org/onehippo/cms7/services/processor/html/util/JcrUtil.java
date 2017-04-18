/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html.util;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.onehippo.repository.util.JcrConstants;

public class JcrUtil {

    public static final String PATH_SEPARATOR = "/";

    /**
     * Retrieve the primary item of a node, according to the primary node type hierarchy. Plain JackRabbit only checks
     * the declared items of a type, not the inherited ones.
     *
     * @param node Node with primary item
     * @return primary item
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public static Item getPrimaryItem(final Node node) throws ItemNotFoundException, RepositoryException {
        NodeType primaryType = node.getPrimaryNodeType();
        String primaryItemName = primaryType.getPrimaryItemName();
        while (primaryItemName == null && !JcrConstants.NT_BASE.equals(primaryType.getName())) {
            for (final NodeType nt : primaryType.getSupertypes()) {
                if (nt.getPrimaryItemName() != null) {
                    primaryItemName = nt.getPrimaryItemName();
                    break;
                }
                if (nt.isNodeType(JcrConstants.NT_BASE)) {
                    primaryType = nt;
                }
            }
        }
        if (primaryItemName == null) {
            throw new ItemNotFoundException("No primary item definition found in type hierarchy");
        }
        return node.getSession().getItem(node.getPath() + PATH_SEPARATOR + primaryItemName);
    }
}
