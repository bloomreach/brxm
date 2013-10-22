package org.onehippo.cms7.essentials.dashboard.utils.wicket;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * Ripped from org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel to make it work without a wicket/jcr Usersession.
 * The Essentials dashboard does not support Session retrieval through this way.
 * @version "$Id$"
 */
public class JcrNodeTypeModel extends org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel {

    private static final long serialVersionUID = 1L;

    private String type;
    private final PluginContext context;

    public JcrNodeTypeModel(PluginContext context, NodeType nodeType) {
        super(nodeType);
        this.context = context;
        type = nodeType.getName();
    }

    public JcrNodeTypeModel(PluginContext context, String type) {
        super(type);
        this.context = context;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    protected NodeType load() {
        NodeType result = null;
        if (type != null) {
            try {
                result = context.getSession().getWorkspace().getNodeTypeManager().getNodeType(type);
            } catch (RepositoryException e) {

            }
        }
        return result;
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeType", type).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JcrNodeTypeModel)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeTypeModel nodeModel = (JcrNodeTypeModel) object;
        return new EqualsBuilder().append(type, nodeModel.type).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(57, 457).append(type).toHashCode();
    }

}
