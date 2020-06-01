/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;

final class WorkflowDescriptorImpl implements WorkflowDescriptor {

    private final WorkflowManagerImpl manager;
    private final String uuid;
    private final String category;
    private final Class workflowClass;
    private WorkflowDefinition definition;
    private Map<String, Serializable> hints = null;

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, WorkflowDefinition definition, Document document) throws RepositoryException {
        this(manager, category, definition, document.getIdentity());
    }

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, WorkflowDefinition definition, Node item) throws RepositoryException {
        this(manager, category, definition, item.getIdentifier());
    }

    private WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, WorkflowDefinition definition, String uuid) throws RepositoryException {
        this.manager = manager;
        this.category = category;
        this.uuid = uuid;
        this.definition = definition;
        workflowClass = definition.getWorkflowClass();
    }

    public String getDisplayName() throws RepositoryException {
        return definition.getDisplayName();
    }

    public String getAttribute(String key) throws RepositoryException {
        return definition.getAttributes().get(key);
    }

    public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
        List<Class<Workflow>> interfaces = new LinkedList<Class<Workflow>>();
        for (Class cls : workflowClass.getInterfaces()) {
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
            } catch (WorkflowException | RemoteException e) {
                throw new RepositoryException("Workflow hints corruption", e);
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
        return getClass().getName() + "[node=" + uuid + ",category=" + category + ",workflowClass=" + workflowClass.getName() + "]";
    }

}
