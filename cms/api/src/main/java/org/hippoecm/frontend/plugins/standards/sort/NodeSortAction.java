/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.sort;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeSortAction implements IClusterable, IDetachable {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(NodeSortAction.class);

    private boolean moveUp, moveDown;
    private JcrNodeModel nodeModel;

    public void setModel(JcrNodeModel nodeModel) {
        moveUp = false;
        moveDown = false;

        try {
            Node node = nodeModel.getNode();
            if (node != null && node.getDepth() > 0 && node.getParent().getPrimaryNodeType().hasOrderableChildNodes()) {
                this.nodeModel = nodeModel;

                NodeIterator siblings = node.getParent().getNodes();
                long size = siblings.getSize();
                long position = -1;
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling.isSame(node)) {
                        position = siblings.getPosition();
                        break;
                    }
                }
                if (position != -1) {
                    moveUp = position > 1;
                    moveDown = position < size;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean moveUp() {
        if (moveUp) { //failsafe
            try {
                Node node = nodeModel.getNode();
                Node parentNode = node.getParent();
                NodeIterator siblings = parentNode.getNodes();
                long position = -1;
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling != null && sibling.isSame(node)) {
                        position = siblings.getPosition();
                        break;
                    }
                }
                Node placedBefore = null;
                siblings = parentNode.getNodes();
                for (int i = 0; i < position - 1; i++) {
                    placedBefore = siblings.nextNode();
                }

                String srcChildRelPath = StringUtils.substringAfterLast(node.getPath(), "/");
                String destChildRelPath = placedBefore == null ? null : StringUtils.substringAfterLast(placedBefore
                        .getPath(), "/");
                parentNode.orderBefore(srcChildRelPath, destChildRelPath);

                moveUp = position > 2;
                moveDown = true;
                return true;

            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean moveDown() {
        if (moveDown) { //failsafe
            try {
                Node node = nodeModel.getNode();
                Node parentNode = node.getParent();
                NodeIterator siblings = parentNode.getNodes();
                Node placedBefore = null;
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling.isSame(node)) {
                        siblings.nextNode();
                        if (siblings.hasNext()) {
                            placedBefore = siblings.nextNode();
                        } else {
                            placedBefore = null;
                        }
                        break;
                    }
                }
                String srcChildRelPath = StringUtils.substringAfterLast(node.getPath(), "/");
                String destChildRelPath = placedBefore == null ? null : StringUtils.substringAfterLast(placedBefore
                        .getPath(), "/");
                parentNode.orderBefore(srcChildRelPath, destChildRelPath);

                moveUp = true;
                moveDown = placedBefore != null;
                return true;
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean canMoveDown() {
        return moveDown;
    }

    public boolean canMoveUp() {
        return moveUp;
    }

    public void detach() {
        if(nodeModel != null) {
            nodeModel.detach();
        }
    }

}
