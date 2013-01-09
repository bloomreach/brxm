/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrNodeTypesProvider extends NodeModelWrapper<Void> implements IDataProvider<NodeType> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrNodeTypesProvider.class);

    public JcrNodeTypesProvider(IModel<Node> nodeModel) {
        super(nodeModel);
    }

    public Iterator<NodeType> iterator(int first, int count) {
        List<NodeType> list = new ArrayList<NodeType>();
        Node node = nodeModel.getObject();
        if (node != null) {
            try {
                NodeType[] nodeTypes = node.getMixinNodeTypes();
                list = Arrays.asList(nodeTypes);
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return list.iterator();
    }

    public IModel<NodeType> model(NodeType object) {
        return new JcrNodeTypeModel(object);
    }

    public int size() {
        int result = 0;
        try {
            if (nodeModel.getObject() != null) {
                NodeType[] nodeTypes = nodeModel.getObject().getMixinNodeTypes();
                result = nodeTypes.length;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
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
