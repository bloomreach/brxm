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

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrNodeTypeModel extends LoadableDetachableModel<NodeType> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrNodeTypeModel.class);

    private String type;

    public JcrNodeTypeModel(NodeType nodeType) {
        super(nodeType);
        type = nodeType.getName();
    }

    public JcrNodeTypeModel(String type) {
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
                UserSession sessionProvider = UserSession.get();
                result = sessionProvider.getJcrSession().getWorkspace().getNodeTypeManager().getNodeType(type);
            } catch (RepositoryException e) {
                log.warn("failed to load " + e.getMessage());
            }
        } else {
            log.error("No type info present");
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
        if (object instanceof JcrNodeTypeModel == false) {
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
