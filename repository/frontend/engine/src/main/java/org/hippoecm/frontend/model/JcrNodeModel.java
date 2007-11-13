/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.repository.api.HippoNode;

public abstract class JcrNodeModel implements IChainingModel, TreeNode {
    private static final long serialVersionUID = 1L;

    // The Item model that is chained by this model using the IChainingModel interface
    protected JcrItemModel itemModel;

    // Constructor

    public JcrNodeModel(Node node) {
        itemModel = new JcrItemModel(node);
    }


    // Convenience methods, not part of an api

    public HippoNode getNode() {
        return (HippoNode) itemModel.getObject();
    }

    public void impersonate(JcrNodeModel model) {
        if (model != null) {
            itemModel = new JcrItemModel(model.getNode());
        }
    }

    public abstract void markReload();


    //Implement IChainingModel
    
    public IModel getChainedModel() {
        return itemModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrItemModel) {
            itemModel = (JcrItemModel)model;
        }
    }

    public Object getObject() {
        return itemModel.getObject();
    }

    public void setObject(Object object) {
        itemModel.setObject(object);
    }

    public void detach() {
        itemModel.detach();
    }
    
    // Override Object

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("item", itemModel).toString();
    }

    public boolean equals(Object object) {
        if (object instanceof JcrNodeModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeModel nodeModel = (JcrNodeModel) object;
        return new EqualsBuilder().append(itemModel, nodeModel.itemModel).isEquals();

    }

    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(itemModel).toHashCode();
    }

}
