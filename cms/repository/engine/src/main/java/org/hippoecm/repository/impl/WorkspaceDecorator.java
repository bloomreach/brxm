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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.ContentHandler;

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
    private DocumentManagerImpl workflowDocumentManager;

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
                    rootSession = (SessionDecorator) factory.getSessionDecorator(session.getRepository(), SessionDecorator.unwrap(session.impersonate(new SimpleCredentials("workflowuser", new char[] { })))); // FIXME: hardcoded workflowuser
                }
            } catch(RepositoryException ex) {
                logger.error("No root session available "+ex.getClass().getName()+": "+ex.getMessage());
                throw new RepositoryException("no root session available", ex);
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
