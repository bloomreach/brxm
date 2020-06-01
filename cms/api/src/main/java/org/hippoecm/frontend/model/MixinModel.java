/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixinModel extends NodeModelWrapper<Boolean> {


    private static final long serialVersionUID = 1L;
    public static final Logger log = LoggerFactory.getLogger(MixinModel.class);

    private String type;

    public MixinModel(IModel<Node> nodeModel, String mixin) {
        super(nodeModel);
        this.type = mixin;
    }

    public Boolean getObject() {
        try {
            return isNodeType();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    public void setObject(Boolean value) {
        try {
            Node node = getNode();
            if (node == null) {
                throw new UnsupportedOperationException();
            }
            if (value) {
                if (!isNodeType()) {
                    node.addMixin(type);
                }
            } else {
                if (isNodeType() && hasMixin()) {
                    node.removeMixin(type);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private boolean isNodeType() throws RepositoryException {
        Node node = getNode();
        if (node == null) {
            return false;
        }
        return node.isNodeType(type);
    }

    private boolean hasMixin() throws RepositoryException {
        Node node = getNode();
        if (node == null) {
            return false;
        }
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            if (nodeType.getName().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInherited() {
        try {
            return isNodeType() && !hasMixin();
        } catch (RepositoryException re) {
            log.error(re.getMessage());
        }
        return false;
    }
}
