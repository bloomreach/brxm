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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.repository.api.HippoNode;

public class JcrNodeModel extends ItemModelWrapper implements IPluginModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private transient boolean parentCached = false;
    private transient JcrNodeModel parent;

    public JcrNodeModel(JcrItemModel model) {
        super(model);
    }

    public JcrNodeModel(Node node) {
        super(node);
    }

    public JcrNodeModel(String path) {
        super(path);
    }

    public JcrNodeModel(IPluginModel model) {
        super((String) model.getMapRepresentation().get("node"));
    }

    public Map<String, Object> getMapRepresentation() {
        Map map = new HashMap();
        map.put("node", itemModel.getPath());
        return map;
    }

    public HippoNode getNode() {
        return (HippoNode) itemModel.getObject();
    }

    public JcrNodeModel getParentModel() {
        if (!parentCached) {
            JcrItemModel parentModel = itemModel.getParentModel();
            if (parentModel != null) {
                parent = new JcrNodeModel(parentModel);
            } else {
                parent = null;
            }
            parentCached = true;
        }
        return parent;
    }

    public JcrNodeModel findRootModel() {
        JcrNodeModel result = this;
        while (result.getParentModel() != null) {
            result = result.getParentModel();
        }
        return result;
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("itemModel", itemModel.toString())
                .toString();
    }

    @Override
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder(57, 433).append(itemModel).toHashCode();
    }

}
