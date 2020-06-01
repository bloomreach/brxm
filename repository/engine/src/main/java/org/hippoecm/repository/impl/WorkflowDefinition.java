/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.RepositoryMapImpl;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CLASSNAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONFIG;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DISPLAY;

class WorkflowDefinition {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinition.class);

    private final Node workflowNode;
    private Map<String, String> attributes;

    WorkflowDefinition(Node workflowNode) {
        this.workflowNode = workflowNode;
    }

    Class<? extends Workflow> getWorkflowClass() throws RepositoryException {
        String classname = workflowNode.getProperty(HIPPO_CLASSNAME).getString();
        Class clazz;
        try {
            clazz = Class.forName(classname);
            if (Workflow.class.isAssignableFrom(clazz)) {
                return clazz;
            } else {
                throw new RepositoryException("Invalid class " + classname + " configured as workflow; it does not implement the Workflow interface");
            }
        } catch (ClassNotFoundException e) {
            throw new RepositoryException("Workflow specified at " + JcrUtils.getNodePathQuietly(workflowNode) + " not present", e);
        }
    }

    String getCategory() throws RepositoryException {
        return workflowNode.getParent().getName();
    }

    String getName() throws RepositoryException {
        return workflowNode.getName();
    }

    String getDisplayName() throws RepositoryException {
        return JcrUtils.getStringProperty(workflowNode, HIPPO_DISPLAY, getName());
    }

    String getPath() throws RepositoryException {
        return workflowNode.getPath();
    }

    Map<String, String> getAttributes() throws RepositoryException {
        if (attributes == null) {
            attributes = new HashMap<>();
            for (Property property : new PropertyIterable(workflowNode.getProperties())) {
                final String propertyName = property.getName();
                if (!propertyName.startsWith("hippo:") && !propertyName.startsWith("hipposys:")) {
                    if (!property.getDefinition().isMultiple()) {
                        attributes.put(propertyName, property.getString());
                    }
                }
            }
            for (Node node : new NodeIterable(workflowNode.getNodes())) {
                final String nodeName = node.getName();
                if (!nodeName.startsWith("hippo:") && !nodeName.startsWith("hipposys:")) {
                    attributes.put(nodeName, node.getPath());
                }
            }
        }
        return attributes;
    }

    RepositoryMap getWorkflowConfiguration() {
        try {
            if (workflowNode.hasNode(HIPPO_CONFIG)) {
                return new RepositoryMapImpl(workflowNode.getNode(HIPPO_CONFIG));
            }
        } catch (RepositoryException e) {
            log.error("Cannot access configuration of workflow defined in " + JcrUtils.getNodePathQuietly(workflowNode), e);
        }
        return new RepositoryMapImpl();
    }

}
