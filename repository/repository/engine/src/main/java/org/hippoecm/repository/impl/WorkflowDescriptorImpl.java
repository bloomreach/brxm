/*
 *  Copyright 2008 Hippo.
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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;

final class WorkflowDescriptorImpl implements WorkflowDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String nodeAbsPath;
    String category;
    protected String displayName;
    protected Map<String, String> attributes;
    protected String serviceName;

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, Node item) throws RepositoryException {
        this.category = category;
        nodeAbsPath = item.getPath();
        try {
            try {
                serviceName = node.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                displayName = node.getProperty(HippoNodeType.HIPPO_DISPLAY).getString();
            } catch (PathNotFoundException ex) {
                WorkflowManagerImpl.log.error("Workflow specification corrupt on node " + nodeAbsPath);
                throw new RepositoryException("workflow specification corrupt", ex);
            }

            attributes = new HashMap<String, String>();
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property p = properties.nextProperty();
                if (!p.getName().startsWith("hippo:")) {
                    attributes.put(p.getName(), p.getString());
                }
            }
        } catch (ValueFormatException ex) {
            WorkflowManagerImpl.log.error("Workflow specification corrupt on node " + nodeAbsPath);
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAttribute(String key) throws RepositoryException {
        return attributes.get(key);
    }

    public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
                    System.err.println("BERRY#x "+serviceName);
        Class impl = Class.forName(serviceName);
        List<Class<Workflow>> interfaces = new LinkedList<Class<Workflow>>();
            System.err.println("BERRY#y "+impl.getInterfaces().length);
        for(Class cls : impl.getInterfaces()) {
            System.err.println("BERRY#z "+cls.getName());
            if(Workflow.class.isAssignableFrom(cls)) {
                            System.err.println("BERRY#zz");
                interfaces.add(cls);
            }
        }
        return interfaces.toArray((Class<Workflow>[]) Array.newInstance(Class.class, interfaces.size()));
    }

    public String toString() {
        return getClass().getName() + "[node=" + nodeAbsPath + ",category=" + category + ",service=" + serviceName
                + ",attributes=" + attributes.toString() + "]";
    }

}
