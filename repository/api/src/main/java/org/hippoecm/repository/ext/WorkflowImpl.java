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
package org.hippoecm.repository.ext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;

/**
 * Implementors of a work-flow in the repository must extend from the WorkflowImpl base type.
 */
public abstract class WorkflowImpl implements Remote, Workflow
{
    private Node node;

    /**
     * Work-flow context in use, which ought to be not public accessible.  Use getWorkflowContext instead.
     */
    protected WorkflowContext context;

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     * @throws RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public WorkflowImpl() throws RemoteException {
    }

    /**
     * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
     * @param context the new context that should be used
     */
    final public void setWorkflowContext(WorkflowContext context) {
        this.context = context;
    }

    /**
     * <b>This call is not part of the public API</b><p/>
     * @param node the backing Node for this workflow
     */
    public void setNode(Node node) throws RepositoryException {
        this.node = node;
    }

    /**
     * @return the backing Node of this workflow
     */
    protected Node getNode() {
        return node;
    }

    /**
     * @return the ensured to be checked out backing Node of this Document
     */
    protected Node getCheckedOutNode() throws RepositoryException {
        if (node != null) {
            JcrUtils.ensureIsCheckedOut(node);
        }
        return node;
    }

    final protected WorkflowContext getWorkflowContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String,Serializable> hints() throws WorkflowException {
        return hints(this);
    }

    static Map<String,Serializable> hints(Workflow workflow) {
        Map<String,Serializable> map = new TreeMap<String,Serializable>();
        for(Class cls : workflow.getClass().getInterfaces()) {
            if(Workflow.class.isAssignableFrom(cls)) {
                for(Method method : cls.getDeclaredMethods()) {
                    String methodName = method.getName();
                    if(methodName.equals("hints")) {
                        map.put(methodName, new Boolean(true));
                    }
                }
            }
        }
        return map;
    }
}
