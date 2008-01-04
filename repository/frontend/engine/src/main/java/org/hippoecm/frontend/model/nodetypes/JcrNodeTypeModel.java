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
package org.hippoecm.frontend.model.nodetypes;

import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class JcrNodeTypeModel extends AbstractReadOnlyModel {
    private static final long serialVersionUID = 1L;

    private String name;

    public JcrNodeTypeModel(NodeType nodeType) {
        name = nodeType.getName();
    }

    @Override
    public Object getObject() {
        return name;
    }


    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("name", name)
            .toString();
     }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrNodeTypeModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeTypeModel valueModel = (JcrNodeTypeModel) object;
        return new EqualsBuilder()
            .append(name, valueModel.name)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(73, 217)
            .append(name)
            .toHashCode();
    }

}
