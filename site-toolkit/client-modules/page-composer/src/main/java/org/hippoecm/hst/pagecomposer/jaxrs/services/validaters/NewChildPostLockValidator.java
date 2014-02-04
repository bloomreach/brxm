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
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.Operation;

public class NewChildPostLockValidator extends PostLockValidator {

    private final String newNodeName;

    public NewChildPostLockValidator(final String parentId,
                                     final String newNodeName,
                                     final Operation operation,
                                     final String itemNodeType,
                                     final String rootNodeType) {

        super(parentId, operation, itemNodeType, rootNodeType);
        this.newNodeName = newNodeName;
    }


    protected Node checkNodeForLock(final Session session, final String parentId) throws RepositoryException {
        Node parent = session.getNodeByIdentifier(parentId);
        return parent.getNode(newNodeName);
    }
}
