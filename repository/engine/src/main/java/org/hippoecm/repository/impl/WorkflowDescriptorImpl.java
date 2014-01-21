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
package org.hippoecm.repository.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;

final class WorkflowDescriptorImpl implements WorkflowDescriptor {

    private final WorkflowManagerImpl manager;
    private final String uuid;
    private final String category;
    private final String displayName;
    private final Map<String, String> attributes;
    private final String serviceName;
    private Map<String, Serializable> hints = null;

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, Document document) throws RepositoryException {
        this(manager, category, node, document.getIdentity());
    }

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, Node item) throws RepositoryException {
        this(manager, category, node, item.getIdentifier());
    }

    private WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, String uuid) throws RepositoryException {
        this.manager = manager;
        this.category = category;
        this.uuid = uuid;
        try {
            try {
                serviceName = node.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                displayName = node.getProperty(HippoNodeType.HIPPO_DISPLAY).getString();
            } catch (PathNotFoundException ex) {
                WorkflowManagerImpl.log.error("Workflow specification corrupt on node " + uuid);
                throw new RepositoryException("workflow specification corrupt", ex);
            }

            attributes = new HashMap<String, String>();
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
        } catch (ValueFormatException ex) {
            WorkflowManagerImpl.log.error("Workflow specification corrupt on node " + uuid);
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAttribute(String key) throws RepositoryException {
        if (key == null) {
            StringBuilder sb = new StringBuilder();
            for (String k : attributes.keySet()) {
                sb.append(" ");
                sb.append(k);
            }
            return sb.toString();
        }
        return attributes.get(key);
    }

    public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
        Class impl = Class.forName(serviceName);
        List<Class<Workflow>> interfaces = new LinkedList<Class<Workflow>>();
        for (Class cls : impl.getInterfaces()) {
            if (Workflow.class.isAssignableFrom(cls)) {
                interfaces.add(cls);
            }
        }
        return interfaces.toArray((Class<Workflow>[]) Array.newInstance(Class.class, interfaces.size()));
    }

    public Map<String, Serializable> hints() throws RepositoryException {
        if (this.hints == null) {
            try {
                this.hints = manager.getWorkflow(this).hints();
            } catch (WorkflowException ex) {
                throw new RepositoryException("Workflow hints corruption", ex);
            } catch (RemoteException ex) {
                throw new RepositoryException("Workflow hints corruption", ex);
            }
        }
        return this.hints;
    }

    public String getCategory() {
        return category;
    }

    public String getUuid() {
        return uuid;
    }

    public String toString() {
        return getClass().getName() + "[node=" + uuid + ",category=" + category + ",service=" + serviceName + ",attributes=" + attributes.toString() + "]";
    }

}
