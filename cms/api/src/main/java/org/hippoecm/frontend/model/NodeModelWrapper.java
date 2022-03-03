/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;

public abstract class NodeModelWrapper<T> implements IModel<T> {

    protected JcrNodeModel nodeModel;

    public NodeModelWrapper(IModel<Node> nodeModel) {
        if (!(nodeModel instanceof JcrNodeModel)) {
            this.nodeModel = new JcrNodeModel(nodeModel.getObject());
        } else {
            this.nodeModel = (JcrNodeModel) nodeModel;
        }
    }

    public NodeModelWrapper(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public IModel<Node> getNodeModel() {
        return nodeModel;
    }

    public void setNodeModel(IModel<Node> model) {
        if (model instanceof JcrNodeModel) {
            nodeModel = (JcrNodeModel) model;
        }
    }

    protected Node getNode() {
        return nodeModel.getNode();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) this;
    }

    @Override
    public void setObject(T object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach() {
        if(nodeModel != null) {
            nodeModel.detach();
        }
    }

}
