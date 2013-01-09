/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrowserHelper {

    static final Logger log = LoggerFactory.getLogger(BrowserHelper.class);
    
    private BrowserHelper() {}
    
    public static boolean isFolder(IModel<Node> nodeModel) {
        if (nodeModel.getObject() != null) {
            try {
                Node node = nodeModel.getObject();
                if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    if (!node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                        return false;
                    }
                    String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    node = node.getSession().getNodeByUUID(uuid);
                }
                if (node.isNodeType(HippoStdNodeType.NT_FOLDER) || node.isNodeType(HippoStdNodeType.NT_DIRECTORY)
                        || node.isNodeType(HippoNodeType.NT_NAMESPACE)
                        || node.isNodeType(HippoNodeType.NT_FACETBASESEARCH) || node.isNodeType("rep:root")) {
                    return true;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }
        return true;
    }

    public static boolean isHandle(IModel<Node> nodeModel) {
        if (nodeModel.getObject() != null) {
            try {
                Node node = nodeModel.getObject();
                return node.isNodeType(HippoNodeType.NT_HANDLE) || node.isNodeType(HippoNodeType.NT_FACETRESULT);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    public static JcrNodeModel getParent(IModel<Node> model) {
        JcrNodeModel parentModel = ((JcrNodeModel) model).getParentModel();
        if (parentModel == null) {
            return new JcrNodeModel((Node) null);
        }
        try {
            // skip facetresult nodes in hierarchy
            Node parent = parentModel.getNode();
            if (parent.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                return new JcrNodeModel(parent.getParent());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return parentModel;
    }

}
