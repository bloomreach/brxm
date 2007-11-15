package org.hippoecm.frontend.model.nodetypes;

import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.AbstractReadOnlyModel;

// TODO: Obviously there's more to a NodeType than just a read-only name

public class JcrNodeTypeModel extends AbstractReadOnlyModel {
    private static final long serialVersionUID = 1L;
    
    private String name;
    
    public JcrNodeTypeModel(NodeType nodeType) {
        name = nodeType.getName();
    }

    public Object getObject() {
        return name;
    }
    
    
    // override Object
    
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("name", name)
            .toString();
     }
    
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
    
    public int hashCode() {
        return new HashCodeBuilder(73, 217)
            .append(name)
            .toHashCode();
    }

}
