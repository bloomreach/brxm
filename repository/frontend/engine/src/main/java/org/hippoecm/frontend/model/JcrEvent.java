package org.hippoecm.frontend.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class JcrEvent implements IClusterable {
    private static final long serialVersionUID = 1L;

    private JcrNodeModel nodeModel;

    public JcrEvent(JcrNodeModel nodeModel, boolean structureChanged) {
        this.nodeModel = nodeModel;
        if (structureChanged) {
            nodeModel.markDirty();
        }
    }

    public JcrNodeModel getModel() {
        return nodeModel;
    }

    // override Object

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel).toString();
    }

    public boolean equals(Object object) {
        if (object instanceof JcrEvent == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrEvent event = (JcrEvent) object;
        return new EqualsBuilder().append(nodeModel, event.nodeModel).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(11, 99).append(nodeModel).toHashCode();
    }

}