/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import javax.jcr.Node;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class NodeModelWrapper<T> implements IChainingModel<T> {

    private static final long serialVersionUID = 1L;

    protected JcrNodeModel nodeModel;

    public NodeModelWrapper(IModel<Node> nodeModel) {
        if (!(nodeModel instanceof JcrNodeModel)) {
            this.nodeModel = new JcrNodeModel(nodeModel.getObject());
        } else {
            this.nodeModel = (JcrNodeModel) nodeModel;
        }
    }

    // Implement IChainingModel

    public IModel<Node> getChainedModel() {
        return nodeModel;
    }

    public void setChainedModel(IModel<?> model) {
        if (model instanceof JcrNodeModel) {
            nodeModel = (JcrNodeModel) model;
        }
    }

    protected Node getNode() {
        return nodeModel.getNode();
    }
    
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) this;
    }

    public void setObject(T object) {
        throw new UnsupportedOperationException();
    }

    public void detach() {
        if(nodeModel != null) {
            nodeModel.detach();
        }
    }

}
