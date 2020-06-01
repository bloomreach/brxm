/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.tree.icon;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJcrTreeNodeIconProvider implements ITreeNodeIconProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractJcrTreeNodeIconProvider.class);

    /**
     * Checks if the wrapped jcr node is a virtual node
     * @return true if the node is virtual else false
     */
    public boolean isVirtual(IJcrTreeNode node) {
        IModel<Node> nodeModel = node.getNodeModel();
        if (nodeModel == null) {
            return false;
        }
        Node jcrNode = nodeModel.getObject();
        if (jcrNode == null || !(jcrNode instanceof HippoNode)) {
            return false;
        }
        HippoNode hippoNode = (HippoNode) jcrNode;
        try {
            return hippoNode.isVirtual();
        } catch (RepositoryException e) {
            log.debug("Cannot determine whether node '{}' is virtual, assuming it's not", JcrUtils.getNodePathQuietly(hippoNode), e);
            return false;
        }
    }

}
