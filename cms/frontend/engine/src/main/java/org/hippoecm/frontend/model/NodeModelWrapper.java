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
package org.hippoecm.frontend.model;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class NodeModelWrapper implements IChainingModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected JcrNodeModel nodeModel;

    public NodeModelWrapper(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    // Implement IChainingModel

    public IModel getChainedModel() {
        return nodeModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrNodeModel) {
            nodeModel = (JcrNodeModel) model;
        }
    }

    public Object getObject() {
        return nodeModel.getObject();
    }

    public void setObject(Object object) {
        nodeModel.setObject(object);
    }

    public void detach() {
        if(nodeModel != null) {
            nodeModel.detach();
        }
    }

}
