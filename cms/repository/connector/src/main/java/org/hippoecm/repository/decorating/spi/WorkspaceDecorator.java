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
package org.hippoecm.repository.decorating.spi;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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

import org.xml.sax.ContentHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.decorating.DocumentManagerDecorator;
import org.hippoecm.repository.decorating.WorkflowManagerDecorator;

public class WorkspaceDecorator extends org.hippoecm.repository.decorating.WorkspaceDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    HippoSession remoteSession = null;
    HippoWorkspace remoteWorkspace = null;
    HierarchyResolver remoteHierarchyResolver = null;;

    public WorkspaceDecorator(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session, workspace);
        remoteSession = ((SessionDecorator)session).getRemoteSession();
        try {
            remoteWorkspace = (HippoWorkspace) remoteSession.getWorkspace();
            remoteHierarchyResolver = remoteWorkspace.getHierarchyResolver();
        } catch(RepositoryException ex) {
        }
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        return new DocumentManagerDecorator() {
                public Session getSession() {
                    return session;
                }
                public Document getDocument(String category, String identifier) throws RepositoryException {
                    return remoteWorkspace.getDocumentManager().getDocument(category, identifier);
                }
            };
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        return new WorkflowManagerDecorator(session) {
                public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return remoteWorkflowManager.getWorkflowDescriptor(category, remoteSession.getRootNode().getNode(item.getPath().substring(1)));
                }
                public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return remoteWorkflowManager.getWorkflow(descriptor);
                }
                public Workflow getWorkflow(String category, Node item) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return remoteWorkflowManager.getWorkflow(category, remoteSession.getRootNode().getNode(item.getPath().substring(1)));
                }
            };
    }

    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return new HierarchyResolver() {
                public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
                    Item item = remoteHierarchyResolver.getItem(ancestor, path, isProperty, last);
                    if(item.isNode()) {
                        item = session.getRootNode().getNode(item.getPath().substring(1));
                    } else {
                        item = session.getRootNode().getProperty(item.getPath().substring(1));
                    }
                    return factory.getItemDecorator(session, item);
                }

                public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException {
                    Item item = remoteHierarchyResolver.getItem(ancestor, path);
                    if(item.isNode()) {
                        item = session.getRootNode().getNode(item.getPath().substring(1));
                    } else {
                        item = session.getRootNode().getProperty(item.getPath().substring(1));
                    }
                    return factory.getItemDecorator(session, item);
                }

                public Property getProperty(Node node, String field) throws RepositoryException {
                    Property property = remoteHierarchyResolver.getProperty(node, field);
                    property = session.getRootNode().getProperty(property.getPath().substring(1));
                    return factory.getPropertyDecorator(session, property);
                }

                public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
                    Property property = remoteHierarchyResolver.getProperty(node, field, last);
                    property = session.getRootNode().getProperty(property.getPath().substring(1));
                    return factory.getPropertyDecorator(session, property);
                }

                public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
                    node = remoteHierarchyResolver.getNode(node, field);
                    node = session.getRootNode().getNode(node.getPath().substring(1));
                    return factory.getNodeDecorator(session, node);
                }
            };
    }
}



