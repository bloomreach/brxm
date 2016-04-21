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

package org.hippoecm.frontend.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_NAMED;

public class DocumentUtils {

    public static IModel<String> getDocumentNameModel(IModel<Node> model) throws RepositoryException {
        return getDocumentNameModel(model.getObject());
    }

    public static IModel<String> getDocumentNameModel(final Node node) throws RepositoryException {
        if (node == null) {
            return null;
        }
        if (!node.isNodeType(NT_NAMED)) {
            final Node parent = node.getParent();
            if (parent != null && parent.isNodeType(NT_NAMED) && parent.isNodeType(NT_HANDLE)) {
                return new NodeNameModel(new JcrNodeModel(parent));
            } else {
                return new Model<>(node.getName());
            }
        } else {
            return new NodeNameModel(new JcrNodeModel(node));
        }
    }

}
