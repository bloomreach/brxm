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

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;

import org.hippoecm.repository.decorating.DecoratorFactory;

/**
 * Simple workspace decorator.
 */
public class WorkspaceDecorator extends org.hippoecm.repository.decorating.WorkspaceDecorator implements HippoWorkspace {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger logger = LoggerFactory.getLogger(WorkspaceDecorator.class);

    /** The underlying workspace instance. */
    protected final Workspace workspace;
    protected Session session;
    protected DocumentManager documentManager;
    protected WorkflowManager workflowManager;
    private SessionDecorator rootSession;

    /**
     * Creates a workspace decorator.
     *
     * @param factory
     * @param session
     * @param workspace
     */
    public WorkspaceDecorator(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session, workspace);
        this.session = session;
        this.workspace = workspace;
        documentManager = null;
        workflowManager = null;
        rootSession = null;
    }

    @Override
    protected void finalize() {
        if(rootSession != null) {
            if(rootSession.isLive()) {
                rootSession.logout();
            }
            rootSession = null;
        }
    }
    
    @Override
    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            documentManager = new DocumentManagerImpl(session);
        }
        return documentManager;
    }

    @Override
    public WorkflowManager getWorkflowManager() throws RepositoryException {
        if(rootSession == null) {
            Repository repository = RepositoryDecorator.unwrap(session.getRepository());
            try {
                if(repository instanceof RepositoryImpl) {
                    rootSession = (SessionDecorator) factory.getSessionDecorator(session.getRepository(), session.impersonate(new SimpleCredentials("workflowuser", new char[] { }))); // FIXME: hardcoded workflowuser
                }
            } catch (LoginException ex) {
                logger.debug("User " + session.getUserID() + " is not allowed to impersonate to workflow session", ex);
                throw new AccessDeniedException("User " + session.getUserID() + " is not allowed to obtain the workflow manager", ex);
            } catch(RepositoryException ex) {
                logger.error("Error while trying to obtain workflow session "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new RepositoryException("Error while trying to obtain workflow session", ex);
            }
        }

        if (workflowManager == null) {
            workflowManager = new WorkflowManagerImpl(session, rootSession);
        }

        return workflowManager;
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return new HierarchyResolverImpl();
    }
}
