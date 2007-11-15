package org.hippoecm.frontend.model.nodetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;

public class JcrNodeTypesProvider extends NodeModelWrapper implements IDataProvider {
    private static final long serialVersionUID = 1L;
    
    public JcrNodeTypesProvider(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        Node node = nodeModel.getNode();
        if (node != null) {
            try {
                NodeType[] nodeTypes = node.getMixinNodeTypes();
                list = Arrays.asList(nodeTypes);               
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        NodeType nodeType = (NodeType)object;
        return new JcrNodeTypeModel(nodeType);
    }

    public int size() {
        int result = 0;
        try {
            if (nodeModel.getNode() != null) {
                NodeType[] nodeTypes = nodeModel.getNode().getMixinNodeTypes();
                result = nodeTypes.length;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("nodeModel", nodeModel.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrNodeTypesProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrNodeTypesProvider nodeTypesProvider = (JcrNodeTypesProvider) object;
        return new EqualsBuilder()
            .append(nodeModel, nodeTypesProvider.nodeModel)
            .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 111)
            .append(nodeModel)
            .toHashCode();
    }

}
