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
package org.hippoecm.frontend.template.model;

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ItemModel extends NodeModelWrapper implements IPluginModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ItemModel.class);

    private ItemDescriptor descriptor;

    //  Constructor
    public ItemModel(ItemDescriptor descriptor, JcrNodeModel parent) {
        super(parent);
        this.descriptor = descriptor;
    }

    public ItemModel(IPluginModel model) {
        super(new JcrNodeModel(model));
        Map<String, Object> map = model.getMapRepresentation();
        this.descriptor = (ItemDescriptor) map.get("item");
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = getNodeModel().getMapRepresentation();
        map.put("item", descriptor);
        return map;
    }

    public ItemDescriptor getDescriptor() {
        return descriptor;
    }

    // override Object methods

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor).append(
                "node", getNodeModel()).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ItemModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        ItemModel itemModel = (ItemModel) object;
        return new EqualsBuilder().append(descriptor, itemModel.descriptor).
            append(nodeModel.getItemModel(), itemModel.nodeModel.getItemModel()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 67).append(descriptor).append(nodeModel.getItemModel()).toHashCode();
    }
}
