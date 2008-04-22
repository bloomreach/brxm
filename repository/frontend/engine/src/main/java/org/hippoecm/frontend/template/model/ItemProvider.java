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

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides FieldModel instances based on a template descriptor.
 */
public class ItemProvider extends AbstractProvider<ItemModel> implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ItemProvider.class);

    private ItemDescriptor descriptor;

    // Constructor

    public ItemProvider(ItemDescriptor descriptor, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.descriptor = descriptor;
    }

    public void setDescriptor(ItemDescriptor descriptor) {
        this.descriptor = descriptor;
        detach();
    }

    // internal (lazy) loading of fields

    @Override
    protected void load() {
        if (elements != null) {
            return;
        }

        JcrNodeModel model = getNodeModel();
        elements = new LinkedList<ItemModel>();
        if (descriptor != null) {
            Iterator<ItemDescriptor> iter = descriptor.getItems().iterator();
            while (iter.hasNext()) {
                ItemDescriptor item = iter.next();
                TemplateDescriptor template = item.getTemplate();
                if(template == null) {
                    template = (TemplateDescriptor) item;
                }
                if (item.getField() == null
                        || template.getTypeDescriptor().getField(item.getField()) != null) {
                    elements.addLast(new ItemModel(item, model));
                } else {
                    log.debug("template field does not exist in type");
                }
            }
        }
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ItemProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        ItemProvider itemProvider = (ItemProvider) object;
        return new EqualsBuilder().append(descriptor, itemProvider.descriptor).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(descriptor).toHashCode();
    }
}
