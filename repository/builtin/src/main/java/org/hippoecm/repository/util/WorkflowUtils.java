/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;

public final class WorkflowUtils {

    private WorkflowUtils() {}

    /**
     * Get the containing folder of a document.
     *
     * @param document either a document, a handle or a folder
     * @param session the session to use to get the containing folder for
     * @return  the folder containing this document or the root document
     * @throws RepositoryException
     */
    public static Document getContainingFolder(Document document, Session session) throws RepositoryException {
        return new Document(getContainingFolder(document.getNode(session)));
    }

    /**
     * Get the containing folder node of a document node.
     *
     * @param node  either a node representing a document, a handle, or a folder
     * @return  the folder node containing this document node or the root document
     * @throws RepositoryException
     */
    public static Node getContainingFolder(Node node) throws RepositoryException {
        final Node parent = node.getParent();
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return parent;
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT))        {
            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                return parent.getParent();
            } else if (parent.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return parent;
            }
        }
        return node.getSession().getRootNode();
    }

}
