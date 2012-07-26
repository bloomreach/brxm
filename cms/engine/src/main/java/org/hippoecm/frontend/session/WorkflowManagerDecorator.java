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
package org.hippoecm.frontend.session;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowManagerDecorator implements WorkflowManager {

    private WorkflowManager delegate;
    private ClassLoader loader;

    public WorkflowManagerDecorator(WorkflowManager delegate, ClassLoader loader) {
        this.delegate = delegate;
        this.loader = loader;
    }

    public Session getSession() throws RepositoryException {
        return delegate.getSession();
    }

    public Workflow getWorkflow(String category, Node item) throws MappingException, RepositoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return delegate.getWorkflow(category, item);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public Workflow getWorkflow(String category, Document document) throws MappingException, RepositoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return delegate.getWorkflow(category, document);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws MappingException, RepositoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return delegate.getWorkflow(descriptor);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return delegate.getWorkflowDescriptor(category, item);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return delegate.getWorkflowDescriptor(category, document);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public WorkflowManager getContextWorkflowManager(Object specification) throws RepositoryException {
        return delegate.getContextWorkflowManager(specification);
    }
}
