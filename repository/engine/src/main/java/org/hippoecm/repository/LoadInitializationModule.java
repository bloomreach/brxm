/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.version.VersionException;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.ext.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadInitializationModule extends Thread implements DaemonModule, EventListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    /** Query for finding initialization items
     * TODO: move this query into the repository as query node
     * FIXME: this assumes all initailizeitem are also system, but they aren't necessarily
     */
    public final static String GET_INITIALIZE_ITEMS =
        "SELECT * FROM hipposys:initializeitem " +
        "WHERE jcr:path = '/hippo:configuration/hippo:initialize/%' AND (" + HippoNodeType.HIPPO_NAMESPACE + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_NODETYPESRESOURCE + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_NODETYPES + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_CONTENTRESOURCE + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_CONTENT + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_CONTENTDELETE + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_CONTENTPROPSET + " IS NOT NULL " +
        "OR " + HippoNodeType.HIPPO_CONTENTPROPADD + " IS NOT NULL) " +
        "ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC";

    private Session session;
    private boolean keepRunning = true;
    private boolean doCycle;

    public void initialize(Session session) throws RepositoryException {
        this.session = session;
        keepRunning = true;
        doCycle = false;
        ObservationManager obMgr = session.getWorkspace().getObservationManager();
        start();
        obMgr.addEventListener(this, Event.NODE_ADDED | Event.PROPERTY_ADDED, "/"
                + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH,
                true, null, null, true);
    }

    public void shutdown() {
        // stop all listeners on refreshSession
        if (session != null && session.isLive()) {
            try {
                ObservationManager obMgr = session.getWorkspace().getObservationManager();
                EventListenerIterator elIter = obMgr.getRegisteredEventListeners();
                while (elIter.hasNext()) {
                    EventListener el = elIter.nextEventListener();
                    log.debug("Removing EventListener");
                    obMgr.removeEventListener(el);
                }
            } catch (Exception ex) {
                log.error("Error while removing listener: " + ex.getMessage(), ex);
            }
            session.logout();
        }
        doCycle = false;
        keepRunning = false;
        session.logout();
        interrupt();
    }

    public void run() {
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                if (session != null && session.isLive()) {
                    session.refresh(true);
                    if (doCycle) {
                        cycle();
                        doCycle = false;
                    }
                } else {
                    log.info("Session is gone. Stopping event listener refresher.");
                    keepRunning = false;
                    break;
                }
            } catch (RepositoryException e) {
                log.error("Error while refreshing session. Stopping event listener refresher.", e);
                keepRunning = false;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
    }
    
    protected void cycle() {
        refresh(session);
    }

    public void onEvent(EventIterator events) {
        log.debug("received initialization change event.");
        doCycle = true; // refresh(session);
    }

    static void refresh(Session session) {
        try {
            if (session == null || !session.isLive()) {
                log.warn("Unable to refresh initialize nodes, no session available");
                return;
            }

            Workspace workspace = session.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

            session.refresh(false);
            Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(GET_INITIALIZE_ITEMS, Query.SQL);
            NodeIterator initializeItems = getInitializeItems.execute().getNodes();
            Node configurationNode = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
            Node initializationNode = configurationNode.getNode(HippoNodeType.INITIALIZE_PATH);

            while (initializeItems.hasNext()) {
                Node node = initializeItems.nextNode();
                log.info("Initializing configuration from " + node.getName());
                try {
                    // Namespace
                    if (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found namespace configuration");
                        }
                        Property p = node.getProperty(HippoNodeType.HIPPO_NAMESPACE);
                        String namespace = p.getString();
                        log.info("Initializing namespace: " + node.getName() + " " + namespace);
                        // Add namespace if it doesn't exist
                        LocalHippoRepository.initializeNamespace(nsreg, node.getName(), namespace);
                        p.remove();
                    }

                    // Nodetypes from FILE
                    if (node.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found nodetypes resource configuration");
                        }
                        Property p = node.getProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE);
                        String cndName = p.getString();
                        InputStream cndStream = null;
                        if (cndName.startsWith("file:")) {
                            if (cndName.startsWith("file://")) {
                                cndName = cndName.substring(6);
                            } else if (cndName.startsWith("file:/")) {
                                cndName = cndName.substring(5);
                            } else if (cndName.startsWith("file:")) {
                                cndName = "/" + cndName.substring(5);
                            }
                            File localFile = new File(cndName);
                            try {
                                cndStream = new BufferedInputStream(new FileInputStream(localFile));
                            } catch (FileNotFoundException e) {
                                log.error("Nodetypes initialization file not found: " + cndName, e);
                            }
                        } else {
                            if (node.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                                URL resource = new URL(node.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)
                                        .getString());
                                resource = new URL(resource, cndName);
                                cndName = resource.toString();
                                cndStream = resource.openStream();
                            } else {
                                cndStream = LocalHippoRepository.class.getResourceAsStream(cndName);
                            }
                        }
                        if (cndStream == null) {
                            log.error("Cannot locate nodetype configuration '" + cndName + "', initialization skipped");
                        } else {
                            log.info("Initializing nodetypes from: " + cndName);
                            LocalHippoRepository.initializeNodetypes(workspace, cndStream, cndName);
                            p.remove();
                        }
                    }

                    // Nodetypes from node
                    if (node.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found nodetypes configuration");
                        }
                        Property p = node.getProperty(HippoNodeType.HIPPO_NODETYPES);
                        String cndName = "<<internal>>";
                        InputStream cndStream = p.getStream();
                        if (cndStream == null) {
                            log.error("Cannot get stream for nodetypes definition property.");
                        } else {
                            log.info("Initializing nodetypes from nodetypes property.");
                            LocalHippoRepository.initializeNodetypes(workspace, cndStream, cndName);
                            p.remove();
                        }
                    }

                    // Delete content
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTDELETE)) {
                        Property p = node.getProperty(HippoNodeType.HIPPO_CONTENTDELETE);
                        String path = p.getString();
                        log.warn("Delete content in initialization has been deprecrated: " + node.getName() + " " + path);
                        boolean immediateSave;
                        if(node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) || node.hasProperty(HippoNodeType.HIPPO_CONTENT))
                            immediateSave = false;
                        else
                            immediateSave = true;
                        log.info("Delete content in initialization: " + node.getName() + " " + path);
                        LocalHippoRepository.removeNodecontent(session, path, immediateSave);
                        p.remove();
                    }

                    // Content from file
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found content resource configuration");
                        }

                        Property contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE);
                        String contentName = contentProperty.getString();

                        InputStream contentStream = null;
                        if (contentName.startsWith("file:")) {
                            if (contentName.startsWith("file://")) {
                                contentName = contentName.substring(6);
                            } else if (contentName.startsWith("file:/")) {
                                contentName = contentName.substring(5);
                            } else if (contentName.startsWith("file:")) {
                                contentName = "/" + contentName.substring(5);
                            }
                            File localFile = new File(contentName);
                            try {
                                contentStream = new BufferedInputStream(new FileInputStream(localFile));
                            } catch (FileNotFoundException e) {
                                log.error("Content resource file not found: " + contentStream, e);
                            }
                        } else {
                            if (node.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                                URL resource = new URL(node.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)
                                        .getString());
                                resource = new URL(resource, contentName);
                                contentName = resource.toString();
                                contentStream = resource.openStream();
                            } else {
                                contentStream = LocalHippoRepository.class.getResourceAsStream(contentName);
                            }
                        }

                        if (contentStream == null) {
                            log.error("Cannot locate content configuration '" + contentName
                                    + "', initialization skipped");
                        } else {
                            String root = "/";
                            Property rootProperty = null;
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                            }

                            // verify that content root is not under the initialization node
                            String initPath = initializationNode.getPath();
                            if (root.startsWith(initPath)) {
                                log.error("Refusing to extract content to " + root);
                            } else {
                                log.info("Initializing content from: " + contentName + " to " + root);
                                LocalHippoRepository.initializeNodecontent(session, root, contentStream, contentName);
                            }
                            rootProperty.remove();
                            contentProperty.remove();
                        }
                    }

                    // CONTENT FROM NODE
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found content configuration");
                        }

                        Property contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENT);
                        String contentName = "<<internal>>";
                        InputStream contentStream = contentProperty.getStream();

                        if (contentStream == null) {
                            log.error("Cannot locate content configuration '" + contentName
                                    + "', initialization skipped");
                        } else {
                            String root = "/";
                            Property rootProperty = null;
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                            }

                            // verify that content root is not under the initialization node
                            String initPath = initializationNode.getPath();
                            if (root.startsWith(initPath)) {
                                log.error("Refusing to extract content to " + root);
                            } else {
                                log.info("Initializing content from: " + contentName + " to " + root);
                                LocalHippoRepository.initializeNodecontent(session, root, contentStream, contentName + ":"
                                        + node.getPath());
                            }
                            rootProperty.remove();
                            contentProperty.remove();
                        }
                    }

                    // SET OR ADD PROPERTY CONTENT
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTPROPSET) || node.hasProperty(HippoNodeType.HIPPO_CONTENTPROPADD)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found content property set/add configuration");
                        }
                        LinkedList<String> newValues = new LinkedList<String>();
                        Property contentSetProperty = (node.hasProperty(HippoNodeType.HIPPO_CONTENTPROPSET) ?
                                                       node.getProperty(HippoNodeType.HIPPO_CONTENTPROPSET) : null);
                        Property contentAddProperty = (node.hasProperty(HippoNodeType.HIPPO_CONTENTPROPADD) ?
                                                       node.getProperty(HippoNodeType.HIPPO_CONTENTPROPADD) : null);
                        if (contentSetProperty != null) {
                            if (contentSetProperty.isMultiple()) {
                                for (Value value : contentSetProperty.getValues())
                                    newValues.add(value.getString());
                            } else
                                newValues.add(contentSetProperty.getString());
                        }
                        if (contentAddProperty != null) {
                            if (contentAddProperty.isMultiple()) {
                                for (Value value : contentAddProperty.getValues())
                                    newValues.add(value.getString());
                            } else
                                newValues.add(contentAddProperty.getString());
                        }
                        String root = "/";
                        Property rootProperty = null;
                        if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                            root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                        }
                        log.info("Initializin content set/add property " + root);
                        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                        HierarchyResolver hierarchyResolver = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver();
                        Property property = hierarchyResolver.getProperty(session.getRootNode(), root.substring(1), last);
                        if (property == null) {
                            String propertyName = root.substring(root.lastIndexOf("/") + 1);
                            if (!root.substring(0, root.lastIndexOf("/")).equals(last.node.getPath())) {
                                throw new PathNotFoundException(root);
                            }
                            boolean isMultiple = false;
                            boolean isSingle = false;
                            Set<NodeType> nodeTypes = new TreeSet<NodeType>();
                            nodeTypes.add(last.node.getPrimaryNodeType());
                            for (NodeType nodeType : last.node.getMixinNodeTypes())
                                nodeTypes.add(nodeType);
                            for (NodeType nodeType : nodeTypes) {
                                for (PropertyDefinition propertyDefinition : nodeType.getPropertyDefinitions()) {
                                    if (propertyDefinition.getName().equals("*") || propertyDefinition.getName().equals(propertyName)) {
                                        if (propertyDefinition.isMultiple())
                                            isMultiple = true;
                                        else
                                            isSingle = true;
                                    }
                                }
                            }
                            if (newValues.size() == 1 && contentAddProperty == null && (isSingle || (!isSingle && !isMultiple))) {
                                last.node.setProperty(last.relPath, newValues.get(0));
                            } else if (newValues.isEmpty() && (isSingle || (!isSingle && !isMultiple))) {
                                // no-op, the property does not exist
                            } else {
                                last.node.setProperty(last.relPath, newValues.toArray(new String[newValues.size()]));
                            }
                        } else {
                            if (contentSetProperty == null && property.isMultiple()) {
                                LinkedList<String> currentValues = new LinkedList<String>();
                                for (Value value : property.getValues())
                                    currentValues.add(value.getString());
                                currentValues.addAll(newValues);
                                newValues = currentValues;
                            }
                            if (property.isMultiple()) {
                                property.setValue(newValues.toArray(new String[newValues.size()]));
                            } else if (newValues.size() == 1 && contentAddProperty == null) {
                                property.setValue(newValues.get(0));
                            } else if (newValues.isEmpty()) {
                                property.remove();
                            } else {
                                property.setValue(newValues.toArray(new String[newValues.size()]));
                            }
                        }
                        if (rootProperty != null) {
                            rootProperty.remove();
                        }
                        if (contentSetProperty != null) {
                            contentSetProperty.remove();
                        }
                        if (contentAddProperty != null) {
                            contentAddProperty.remove();
                        }
                    }

                    session.save();
                } catch (MalformedURLException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (IOException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (ParseException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (AccessDeniedException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (ConstraintViolationException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (InvalidItemStateException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (ItemExistsException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (LockException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (NoSuchNodeTypeException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (UnsupportedRepositoryOperationException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (ValueFormatException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (VersionException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } catch (PathNotFoundException ex) {
                    log.error("configuration at specified by " + node.getPath() + " failed", ex);
                } finally {
                    session.refresh(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
