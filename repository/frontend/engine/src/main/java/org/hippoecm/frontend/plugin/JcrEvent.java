package org.hippoecm.frontend.plugin;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class JcrEvent implements IClusterable {
    private static final long serialVersionUID = 1L;

    public static final String NEW_MODEL = "newModel";
    public static final String NEEDS_RELOAD = "needsReload";
    public static final String PURE_GUI = "pureGui";
    
    
    private String type;
    private JcrNodeModel nodeModel;
        
    public JcrEvent(String type, JcrNodeModel nodeModel) {
        this.type = type;
        this.nodeModel = nodeModel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("type", type)
            .append("nodeModel", nodeModel)
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrEvent == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrEvent otherEvent = (JcrEvent) object;
        return new EqualsBuilder()
            .append(type, otherEvent.type)
            .append(nodeModel, otherEvent.nodeModel)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 73)
            .append(type)
            .append(nodeModel)
            .toHashCode();
    }

}
