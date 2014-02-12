/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.hippoecm.repository.RepositoryMapImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkflowDefinition {

    static final Logger log = LoggerFactory.getLogger(WorkflowDefinition.class);

    private final Node node;

    WorkflowDefinition(Node workflowNode) {
        this.node = workflowNode;
    }

    Class<? extends Workflow> getWorkflowClass() throws RepositoryException {
        String classname = node.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
        Class clazz;
        try {
            clazz = Class.forName(classname);
            if (Workflow.class.isAssignableFrom(clazz)) {
                return clazz;
            } else {
                throw new RepositoryException("Invalid class " + classname + " configured as workflow; it does not implement the Workflow interface");
            }
        } catch (ClassNotFoundException e) {
            final String message = "Workflow specified at " + node.getPath() + " not present";
            log.error(message);
            throw new RepositoryException(message, e);
        }
    }

    String getCategory() throws RepositoryException {
        return node.getParent().getName();
    }

    String getName() throws RepositoryException {
        return node.getName();
    }

    String getDisplayName() throws RepositoryException {
        if (node.hasProperty(HippoNodeType.HIPPO_DISPLAY)) {
            return node.getProperty(HippoNodeType.HIPPO_DISPLAY).getString();
        } else {
            return getName();
        }
    }

    String getPath() throws RepositoryException {
        return node.getPath();
    }

    Map<String, String> getAttributes() throws RepositoryException {
        Map<String, String> attributes = new HashMap<String, String>();
        for (PropertyIterator attributeIter = node.getProperties(); attributeIter.hasNext(); ) {
            Property p = attributeIter.nextProperty();
            if (!p.getName().startsWith("hippo:") && !p.getName().startsWith("hipposys:")) {
                if (!p.getDefinition().isMultiple()) {
                    attributes.put(p.getName(), p.getString());
                }
            }
        }
        for (NodeIterator attributeIter = node.getNodes(); attributeIter.hasNext(); ) {
            Node n = attributeIter.nextNode();
            if (!n.getName().startsWith("hippo:") && !n.getName().startsWith("hipposys:")) {
                attributes.put(n.getName(), n.getPath());
            }
        }
        return attributes;
    }

    RepositoryMap getWorkflowConfiguration() {
        try {
            if (node.hasNode("hipposys:config")) {
                return new RepositoryMapImpl(node.getNode("hipposys:config"));
            }
        } catch (RepositoryException ex) {
            try {
                log.error("Cannot access configuration of workflow defined in " + node.getPath());
            } catch (RepositoryException e) {
                log.error("Double access error accessing configuration of workflow");
            }
        }
        return new RepositoryMapImpl();
    }

    boolean isSimpleQueryPostAction() throws RepositoryException {
        return node.isNodeType("hipposys:workflowsimplequeryevent");
    }

    boolean isMethodBoundPostAction() throws RepositoryException {
        return node.isNodeType("hipposys:workflowboundmethodevent");
    }

    Node getEventDocument() throws RepositoryException {
        if (node.hasNode("hipposys:eventdocument")) {
            // TODO
        } else if (node.hasProperty("hipposys:eventdocument")) {
            return node.getProperty("hipposys:eventdocument").getNode();
        }
        return null;
    }

    boolean matchesEventCondition(final String workflowCategory, final String workflowMethod) throws RepositoryException {
        if (node.hasProperty("hipposys:eventconditioncategory")) {
            if (!node.getProperty("hipposys:eventconditioncategory").getString().equals(workflowCategory)) {
                return false;
            }
        }
        if (node.hasProperty("hipposys:eventconditionmethod")) {
            if (!node.getProperty("hipposys:eventconditionmethod").getString().equals(workflowMethod)) {
                return false;
            }
        }
        return true;
    }

    Query getPreConditionQuery() throws RepositoryException {
        if (node.hasNode("hipposys:eventprecondition")) {
            return node.getSession().getWorkspace().getQueryManager().getQuery(node.getNode("hipposys:eventprecondition"));
        } else {
            return null;
        }
    }

    Query getPostConditionQuery() throws RepositoryException {
        if (node.hasNode("hipposys:eventpostcondition")) {
            return node.getSession().getWorkspace().getQueryManager().getQuery(node.getNode("hipposys:eventpostcondition"));
        } else {
            return null;
        }
    }

    String getConditionOperator() throws RepositoryException {
        String conditionOperator = "post\\pre";
        if (node.hasProperty("hipposys:eventconditionoperator")) {
            conditionOperator = node.getProperty("hipposys:eventconditionoperator").getString();
        }
        return conditionOperator;
    }
}
