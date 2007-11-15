package org.hippoecm.frontend.plugin;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class JcrEvent implements IClusterable {
    private static final long serialVersionUID = 1L;

    private boolean structureChanged;
    private JcrNodeModel nodeModel;

    public JcrEvent(JcrNodeModel nodeModel, boolean structureChanged) {
        this.nodeModel = nodeModel;
        this.structureChanged = structureChanged;
        if (structureChanged) {
            nodeModel.markReload();
        }
    }

    public JcrNodeModel getModel() {
        return nodeModel;
    }
    
    public boolean structureChanged() {
        return structureChanged;
    }


    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel).toString();
    }

    @Override
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 99).append(nodeModel).toHashCode();
    }

}