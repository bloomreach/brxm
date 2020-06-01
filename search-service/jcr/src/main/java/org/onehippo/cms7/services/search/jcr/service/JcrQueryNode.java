/*
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.jcr.service;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.ASC_DESC;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.CONSTRAINT;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.EXCLUDES;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.INCLUDES;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.LIMIT;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.NODETYPE;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.OFFSET;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.ORDER_BY;
import static org.onehippo.cms7.services.search.jcr.HippoSearchNodeType.RETURNPARENTNODE;

public class JcrQueryNode extends JcrConstraintNode {

    static final Logger log = LoggerFactory.getLogger(JcrQueryNode.class);

    public static class Ordering {
        private final String property;
        private final String ordering;

        public Ordering(final String property, final String ordering) {
            this.property = property;
            this.ordering = ordering;
        }

        public String getOrdering() {
            return ordering;
        }

        public String getProperty() {
            return property;
        }
    }

    private final Node node;

    public JcrQueryNode(final Node node) {
        super(node);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void clear() {
        try {
            for (Property property : new PropertyIterable(node.getProperties())) {
                if (property.getName().startsWith("hipposearch:")) {
                    property.remove();
                }
            }
            for (Node child : new NodeIterable(node.getNodes(CONSTRAINT))) {
                child.remove();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to clean up query node");
        }
    }

    public boolean isReturnParentNode() {
        try {
            if (node.hasProperty(RETURNPARENTNODE)) {
                return node.getProperty(RETURNPARENTNODE).getBoolean();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read returnparentnode", re);
        }
        return false;
    }

    public void setReturnParentNode(boolean value) {
        try {
            node.setProperty(RETURNPARENTNODE, value);
        } catch (RepositoryException re) {
            log.warn("Unable to store returnparentnode", re);
        }
    }

    public int getLimit() {
        try {
            if (node.hasProperty(LIMIT)) {
                return (int) node.getProperty(LIMIT).getLong();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read limit", re);
        }
        return -1;
    }

    public void setLimit(final int limit) {
        try {
            node.setProperty(LIMIT, limit);
        } catch (RepositoryException re) {
            log.warn("Unable to store limit", re);
        }
    }

    public int getOffset() {
        try {
            if (node.hasProperty(OFFSET)) {
                return (int) node.getProperty(OFFSET).getLong();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to read limit", re);
        }
        return -1;
    }

    public void setOffset(final int offset) {
        try {
            node.setProperty(OFFSET, offset);
        } catch (RepositoryException re) {
            log.warn("Unable to store limit", re);
        }
    }

    public String getNodeType() {
        try {
            if (node.hasProperty(NODETYPE)) {
                return node.getProperty(NODETYPE).getString();
            }
        } catch (RepositoryException re) {
            log.warn("Unable to retrieve node type", re);
        }
        return null;
    }

    public void setNodeType(final String nodeType) {
        try {
            node.setProperty(NODETYPE, nodeType);
        } catch (RepositoryException re) {
            log.warn("Unable to store node type", re);
        }
    }

    public void addOrdering(Ordering ordering) {
        try {
            {
                Value[] properties;
                if (node.hasProperty(ORDER_BY)) {
                    properties =  node.getProperty(ORDER_BY).getValues();
                } else {
                    properties = new Value[0];
                }

                Value[] newProps = new Value[properties.length + 1];
                System.arraycopy(properties, 0, newProps, 0, properties.length);
                newProps[properties.length] = node.getSession().getValueFactory().createValue(ordering.getProperty());
                node.setProperty(ORDER_BY, newProps);
            }
            {
                Value[] orders;
                if (node.hasProperty(ASC_DESC)) {
                    orders = node.getProperty(ASC_DESC).getValues();
                } else {
                    orders = new Value[0];
                }

                Value[] newOrders = new Value[orders.length + 1];
                System.arraycopy(orders, 0, newOrders, 0, orders.length);
                newOrders[orders.length] = node.getSession().getValueFactory().createValue(ordering.getOrdering());
                node.setProperty(ASC_DESC, newOrders);
            }
        } catch (RepositoryException re) {
            log.error("Unable to store order by clause for " + ordering.getProperty());
        }
    }

    public Ordering[] getOrderings() {
        try {
            if (node.hasProperty(ORDER_BY) && node.hasProperty(
                    ASC_DESC)) {
                Value[] properties =  node.getProperty(ORDER_BY).getValues();
                Value[] orders = node.getProperty(ASC_DESC).getValues();
                if (properties.length == orders.length) {
                    Ordering[] orderings = new Ordering[properties.length];
                    for (int i = 0; i < properties.length; i++) {
                        orderings[i] = new Ordering(properties[i].getString(), orders[i].getString());
                    }
                    return orderings;
                } else {
                    log.warn("Properties and orderings have a different length");
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to retrieve orderings", e);
        }
        return new Ordering[0];
    }

    public void addInclude(final String path) {
        try {
            Value[] includes;
            if (node.hasProperty(INCLUDES)) {
                includes = node.getProperty(INCLUDES).getValues();
            } else {
                includes = new Value[0];
            }

            Value[] newIncludes = new Value[includes.length + 1];
            System.arraycopy(includes, 0, newIncludes, 0, includes.length);
            newIncludes[includes.length] = node.getSession().getValueFactory().createValue(path);
            node.setProperty(INCLUDES, newIncludes);
        } catch (RepositoryException re) {
            log.warn("Unable to add included path", re);
        }
    }

    public String[] getIncludes() {
        try {
            Value[] includes;
            if (node.hasProperty(INCLUDES)) {
                includes = node.getProperty(INCLUDES).getValues();
            } else {
                includes = new Value[0];
            }

            String[] paths = new String[includes.length];
            int i = 0;
            for (Value include : includes) {
                paths[i++] = include.getString();
            }
            return paths;
        } catch (RepositoryException re) {
            log.warn("Unable to add included path", re);
        }
        return new String[0];
    }

    public void addExclude(final String path) {
        try {
            Value[] excludes;
            if (node.hasProperty(EXCLUDES)) {
                excludes = node.getProperty(EXCLUDES).getValues();
            } else {
                excludes = new Value[0];
            }

            Value[] newExcludes = new Value[excludes.length + 1];
            System.arraycopy(excludes, 0, newExcludes, 0, excludes.length);
            newExcludes[excludes.length] = node.getSession().getValueFactory().createValue(path);
            node.setProperty(EXCLUDES, newExcludes);
        } catch (RepositoryException re) {
            log.warn("Unable to add included path", re);
        }
    }

    public String[] getExcludes() {
        try {
            Value[] excludes;
            if (node.hasProperty(EXCLUDES)) {
                excludes = node.getProperty(EXCLUDES).getValues();
            } else {
                excludes = new Value[0];
            }

            String[] paths = new String[excludes.length];
            int i = 0;
            for (Value include : excludes) {
                paths[i++] = include.getString();
            }
            return paths;
        } catch (RepositoryException re) {
            log.warn("Unable to add included path", re);
        }
        return new String[0];
    }

}
