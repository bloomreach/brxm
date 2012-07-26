/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.model.nodetypes;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class NodeTypeModelWrapper implements IChainingModel {
    private static final long serialVersionUID = 1L;


    protected JcrNodeTypeModel itemModel;

    public NodeTypeModelWrapper(JcrNodeTypeModel model) {
        itemModel = model;
    }

    public NodeTypeModelWrapper(String type) {
        itemModel = new JcrNodeTypeModel(type);
    }

    public JcrNodeTypeModel getNodeTypeModel() {
        return itemModel;
    }

    // Implement IChainingModel

    public IModel getChainedModel() {
        return itemModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrNodeTypeModel) {
            itemModel = (JcrNodeTypeModel) model;
        }
    }

    public Object getObject() {
        return itemModel.getObject();
    }

    public void setObject(Object object) {
        throw new UnsupportedOperationException("Cannot alter the item of an " + getClass());
    }

    public void detach() {
        itemModel.detach();
    }

}
