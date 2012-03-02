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
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
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
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.ext.DaemonModule;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.onehippo.repository.ManagerServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LoadInitializationModule implements DaemonModule, EventListener {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected static final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    /** Query for finding initialization items
     * TODO: move this query into the repository as query node
     * FIXME: this assumes all initailizeitem are also system, but they aren't necessarily
     */
    private static String GET_INITIALIZE_ITEMS =
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

    private static XmlPullParserFactory factory;

    private Session session;
    private ExecutorService executor;

    static {
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            log.error("Could not get xpp factory instance: " + e.getMessage());
        }
    }

    public void initialize(Session session) throws RepositoryException {
        this.session = session;
        // We really need an undecorated workspace so that we can register a Asynchronous event listener
        // This in its turn guarantees that the search index has received the event prior to this module.
        Workspace workspace = session.getWorkspace();
        workspace = org.hippoecm.repository.decorating.WorkspaceDecorator.unwrap(workspace);
        ObservationManager obMgr = workspace.getObservationManager();
        obMgr.addEventListener(this, Event.NODE_ADDED | Event.PROPERTY_ADDED, "/"
                + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH,
                true, null, null, true);
        executor = Executors.newSingleThreadExecutor();
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
            try {
                executor.shutdown();
                executor.awaitTermination(3, TimeUnit.MINUTES);
            } catch(InterruptedException ex) {
                // deliberate ignore, external timeout
            }
            session.logout();
        }
    }

    public void onEvent(EventIterator events) {
        log.debug("received initialization change event.");
        executor.submit(new Runnable() {
            public void run() {
                log.debug("executing initialization change event.");
                refresh(session);
            }
        });
    }

    static void query(Session session) {
        try {
            Node initializeFolder = session.getRootNode().addNode("initialize");
            session.save();
            LoadInitializationModule.locateExtensionResources(session, initializeFolder);
            session.save();
            Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(
                    "SELECT * FROM hipposys:initializeitem "
                    + "WHERE jcr:path = '/initialize/%' AND (" + HippoNodeType.HIPPO_NAMESPACE + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_NODETYPESRESOURCE + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_NODETYPES + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_CONTENTRESOURCE + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_CONTENT + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_CONTENTDELETE + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_CONTENTPROPSET + " IS NOT NULL "
                    + "OR " + HippoNodeType.HIPPO_CONTENTPROPADD + " IS NOT NULL) "
                    + "ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC", Query.SQL);
            refresh(session, getInitializeItems, true);
            session.refresh(false);
            initializeFolder.remove();
            session.save();
        } catch (IOException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    public static void refresh(Session session) {
        try {
            Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(GET_INITIALIZE_ITEMS, Query.SQL);
            refresh(session, getInitializeItems, false);
        } catch (InvalidQueryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    static void refresh(Session session, Query getInitializeItems, boolean dryRun) {
        try {
            if (session == null || !session.isLive()) {
                log.warn("Unable to refresh initialize nodes, no session available");
                return;
            }

            Workspace workspace = session.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

            session.refresh(false);
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
                        if (!dryRun) {
                            initializeNamespace(nsreg, node.getName(), namespace);
                        }
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
                            if (!dryRun) {
                                initializeNodetypes(workspace, cndStream, cndName);
                            }
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
                            if (!dryRun) {
                                initializeNodetypes(workspace, cndStream, cndName);
                            }
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
                        removeNodecontent(session, path, immediateSave && !dryRun);
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
                                Property reloadProperty = null;
                                if (node.hasProperty(HippoNodeType.HIPPO_RELOADONSTARTUP)) {
                                    reloadProperty = node.getProperty(HippoNodeType.HIPPO_RELOADONSTARTUP);
                                }
                                if (reloadProperty != null && reloadProperty.getBoolean()) {
                                    if (!contentStream.markSupported()) {
                                        contentStream = new BufferedInputStream(contentStream);
                                    }
                                    // inspect the xml file to find out if it is a delta xml and to read the name of the context node we must remove
                                    boolean removeSupported = true;
                                    String contextNodeName = null;
                                    if (factory != null) {
                                        // 8 kb should be more than enough to read the root node
                                        contentStream.mark(8192);
                                        XmlPullParser xpp = factory.newPullParser();
                                        xpp.setInput(contentStream, null);
                                        while(xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                                            if (xpp.getEventType() == XmlPullParser.START_TAG) {
                                                String mergeDirective = xpp.getAttributeValue("http://www.onehippo.org/jcr/xmlimport", "merge");
                                                if (mergeDirective != null && (mergeDirective.equals("combine") || mergeDirective.equals("overlay"))) {
                                                    removeSupported = false;
                                                }
                                                contextNodeName = xpp.getAttributeValue("http://www.jcp.org/jcr/sv/1.0", "name");
                                                break;
                                            }
                                            xpp.next();
                                        }
                                        contentStream.reset();
                                    }
                                    if (removeSupported) {
                                        String path = root.equals("/") ? root + contextNodeName : root + "/" + contextNodeName;
                                        removeNodecontent(session, path, false);
                                    } else {
                                        log.warn("Cannot remove node for reloading content: content resource is a delta xml with combine or overlay directive");
                                    }
                                    reloadProperty.remove();
                                }
                                log.info("Initializing content from: " + contentName + " to " + root);
                                initializeNodecontent(session, root, contentStream, contentName);
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
                                initializeNodecontent(session, root, contentStream, contentName + ":"
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
                        log.info("Initializing content set/add property " + root);
                        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                        HierarchyResolver hierarchyResolver;
                        if (session.getWorkspace() instanceof HippoWorkspace) {
                            hierarchyResolver = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver();
                        } else {
                            hierarchyResolver = ManagerServiceFactory.getManagerService(session).getHierarchyResolver();
                        }
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

                    if (dryRun) {
                        if (log.isDebugEnabled()) {
                            log.debug("configuration as specified by " + node.getName());
                            for (NodeIterator iter = ((HippoSession)session).pendingChanges(); iter.hasNext();) {
                                Node pendingNode = iter.nextNode();
                                if (pendingNode != null) {
                                    log.debug("configuration as specified by " + node.getName() + " modified node " + pendingNode.getPath());
                                }
                            }
                        }
                        session.refresh(false);
                    } else {
                        session.save();
                    }

                } catch (MalformedURLException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (IOException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (ParseException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (AccessDeniedException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (ConstraintViolationException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (InvalidItemStateException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (ItemExistsException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (LockException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (NoSuchNodeTypeException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (UnsupportedRepositoryOperationException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (ValueFormatException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (VersionException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (PathNotFoundException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } catch (XmlPullParserException ex) {
                    log.error("configuration as specified by " + node.getPath() + " failed", ex);
                } finally {
                    session.refresh(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    public static void locateExtensionResources(Session rootSession, Node initializationFolder) throws IOException, RepositoryException {
        List<URL> extensions = new LinkedList<URL>();
        for(Enumeration iter = Thread.currentThread().getContextClassLoader().getResources("org/hippoecm/repository/extension.xml");
            iter.hasMoreElements(); ) {
            URL configurationURL = (URL) iter.nextElement();
            extensions.add(configurationURL);
        }
        for(Enumeration iter = Thread.currentThread().getContextClassLoader().getResources("hippoecm-extension.xml");
            iter.hasMoreElements(); ) {
            URL configurationURL = (URL) iter.nextElement();
            extensions.add(configurationURL);
        }
        /* FIXME: does not seem to be necessary, as long as in the
         * EAR the extensions are all placed in the default place,
         * not in for instance APP-INF/lib
         *     for(Enumeration iter = getClass().getClassLoader().getParent().
         *         getResources("org/hippoecm/repository/extension.xml"); iter.hasMoreElements(); ) {
         *         URL configurationURL = (URL) iter.nextElement();
         *         extensions.addend(configurationURL);
         *     }
         * TODO: Use merge behavior from dereferenced import? [BvdS] [BvH] No not a operation which can be supported by project export
         */
        rootSession.save();
        for(Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            URL configurationURL = (URL) iter.next();
            log.info("Initializing additional configuration content from "+configurationURL);
            try {
                InputStream configurationStream = configurationURL.openStream();
                initializeNodecontent(rootSession, "/hippo:configuration/hippo:temporary", configurationStream, configurationURL.getPath());
                Node mergeInitializationNode = rootSession.getRootNode().
                    getNode("hippo:configuration/hippo:temporary/hippo:initialize");
                for(NodeIterator mergeIter = mergeInitializationNode.getNodes(); mergeIter.hasNext(); ) {
                    Node n = mergeIter.nextNode();
                    long buildNumber = -1;
                    String configurationURLString = configurationURL.toString();
                    if (configurationURLString.endsWith("hippoecm-extension.xml")) {
                        String manifestUrlString = configurationURLString.substring(0, configurationURLString.length() - "hippoecm-extension.xml".length()) + "META-INF/MANIFEST.MF";
                        try {
                            Manifest manifest = new Manifest(new URL(manifestUrlString).openStream());
                            String buildString = manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Build"));
                            if (buildString != null) {
                                buildNumber = Long.parseLong(buildString);
                            }
                        } catch (IOException ex) {
                            // deliberate ignore, manifest file not available so no build number can be obtained
                        }
                    }
                    long existingBuildNumber = -1;
                    if (initializationFolder.hasNode(n.getName()) && initializationFolder.getNode(n.getName()).hasProperty(HippoNodeType.HIPPO_EXTENSIONBUILD))
                        existingBuildNumber = initializationFolder.getNode(n.getName()).getProperty(HippoNodeType.HIPPO_EXTENSIONBUILD).getLong();
                    if (!initializationFolder.hasNode(n.getName()) ||
                        (n.hasProperty(HippoNodeType.HIPPO_RELOADONSTARTUP) && n.getProperty(HippoNodeType.HIPPO_RELOADONSTARTUP).getBoolean() && (buildNumber < 0 || existingBuildNumber < 0 || buildNumber > existingBuildNumber))) {
                        if(initializationFolder.hasNode(n.getName())) {
                            // this occurs when reload is on
                            initializationFolder.getNode(n.getName()).remove();
                        }
                        Node moved = initializationFolder.addNode(n.getName(), HippoNodeType.NT_INITIALIZEITEM);
                        if("hippoecm-extension.xml".equals(configurationURL.getFile().contains("/")
                                       ? configurationURL.getFile().substring(configurationURL.getFile().lastIndexOf("/")+1)
                                       : configurationURL.getFile())) {
                            moved.setProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE, configurationURL.toString());
                            if (buildNumber >= 0) {
                                moved.setProperty(HippoNodeType.HIPPO_EXTENSIONBUILD, buildNumber);
                            }
                        }
                        for (String propertyName : new String[] { HippoNodeType.HIPPO_SEQUENCE, HippoNodeType.HIPPO_NAMESPACE, HippoNodeType.HIPPO_NODETYPESRESOURCE, HippoNodeType.HIPPO_NODETYPES, HippoNodeType.HIPPO_CONTENTRESOURCE, HippoNodeType.HIPPO_CONTENT, HippoNodeType.HIPPO_CONTENTROOT, HippoNodeType.HIPPO_CONTENTDELETE, HippoNodeType.HIPPO_CONTENTPROPSET, HippoNodeType.HIPPO_CONTENTPROPADD, HippoNodeType.HIPPO_RELOADONSTARTUP }) {
                            if(n.hasProperty(propertyName)) {
                                if(n.getProperty(propertyName).getDefinition().isMultiple()) {
                                    moved.setProperty(propertyName, n.getProperty(propertyName).getValues());
                                } else {
                                    moved.setProperty(propertyName, n.getProperty(propertyName).getValue());
                                }
                            }
                        }
                    } else {
                        log.info("Node " + n.getName() + " already exists in initialize folder (source: " + configurationURL.toString() + ")");
                    }
                }
                if(mergeInitializationNode.hasProperty(HippoNodeType.HIPPO_VERSION)) {
                    Set<String> tags = new TreeSet<String>();
                    if (initializationFolder.hasProperty(HippoNodeType.HIPPO_VERSION)) {
                        for (Value value : initializationFolder.getProperty(HippoNodeType.HIPPO_VERSION).getValues()) {
                            tags.add(value.getString());
                        }
                    }
                    Value[] added = mergeInitializationNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues();
                    for (Value value : added) {
                        tags.add(value.getString());
                    }
                    initializationFolder.setProperty(HippoNodeType.HIPPO_VERSION, tags.toArray(new String[tags.size()]));
                }
                mergeInitializationNode.remove();
                rootSession.save();
            } catch (PathNotFoundException ex) {
                log.error("Rejected old style configuration content", ex);
                for(NodeIterator removeTempIter = rootSession.getRootNode().getNode("hippo:configuration/hippo:temporary").getNodes(); removeTempIter.hasNext(); ) {
                    removeTempIter.nextNode().remove();
                }
                rootSession.save();
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (ConstraintViolationException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (InvalidItemStateException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (ItemExistsException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (LockException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (NoSuchNodeTypeException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            } catch (VersionException ex) {
                throw new RepositoryException("Could not initialize repository with configuration content", ex);
            }
        }
    }

    public static void initializeNamespace(NamespaceRegistry nsreg, String prefix, String uri)
            throws UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        try {

            /* Try to remap namespace if a namespace already exists and the uri is similar.
             * This assumes a convention to use in the namespace URI.  It should end with a version
             * number of the nodetypes, such as in http://www.sample.org/nt/1.0.0
             */
            try {
                String currentURI = nsreg.getURI(prefix);
                if (currentURI.equals(uri)) {
                    log.debug("Namespace already exists: " + prefix + ":" + uri);
                    return;
                }
                String uriPrefix = currentURI.substring(0, currentURI.lastIndexOf("/") + 1);
                if(!uriPrefix.equals(uri.substring(0,uri.lastIndexOf("/")+1))) {
                    log.error("Prefix already used for different namespace: " + prefix + ":" + uri);
                    return;
                }
                // do not remap namespace, the upgrading infrastructure must take care of this
                return;
            } catch (NamespaceException ex) {
                if (!ex.getMessage().endsWith("is not a registered namespace prefix.")) {
                    log.error(ex.getMessage() +" For: " + prefix + ":" + uri);
                }
            }

            nsreg.registerNamespace(prefix, uri);

        } catch (NamespaceException ex) {
            if (ex.getMessage().endsWith("mapping already exists")) {
                log.error("Namespace already exists: " + prefix + ":" + uri);
            } else {
                log.error(ex.getMessage()+" For: " + prefix + ":" + uri);
            }
        }
    }

    public static void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws ParseException,
            RepositoryException {
        CompactNodeTypeDefReader<QNodeTypeDefinition,NamespaceMapping> cndReader = new HippoCompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(new InputStreamReader(cndStream), cndName, workspace.getNamespaceRegistry(), new QDefinitionBuilderFactory());
        List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (Iterator<QNodeTypeDefinition> iter = ntdList.iterator(); iter.hasNext();) {
            QNodeTypeDefinition ntd = iter.next();

            EffectiveNodeType effnt = null;
            try {
                effnt = ntreg.registerNodeType(ntd);
            } catch (NamespaceException ex) {
                log.error(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), ex);
            } catch (InvalidNodeTypeDefException ex) {
                if (ex.getMessage().endsWith("already exists")) {
                    try {
                        effnt = ntreg.reregisterNodeType(ntd);
                        log.info("Replaced NodeType: " + ntd.getName().getLocalName());
                    } catch (NamespaceException e) {
                        log.error(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                    } catch (InvalidNodeTypeDefException e) {
                        log.info(e.getMessage() +". In " + cndName +" for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), e);
                    } catch (RepositoryException e) {
                        if (!e.getMessage().equals("not yet implemented")) {
                            log.warn(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                        }
                    }
                } else {
                    log.error(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), ex);
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    log.warn(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), ex);
                }
            }
        }
    }

    public static void removeNodecontent(Session session, String absPath, boolean save) {
        if ("".equals(absPath) || "/".equals(absPath)) {
            log.warn("Not allowed to delete rootNode from initialization.");
            return;
        }

        String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
        try {
            if (relpath.length() > 0) {
                if (session.getRootNode().hasNode(relpath)) {
                    session.getRootNode().getNode(relpath).remove();
                    if (save) {
                        session.save();
                    }
                } else {
                    log.warn("Cannot remove node /" + relpath + ": no such node");
                }
            }
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error while removing content from '" + absPath + "' : " + ex.getMessage(), ex);
            } else {
                log.error("Error while removing content from '" + absPath + "' : " + ex.getMessage());
            }
        }

    }
   
    public static void initializeNodecontent(Session session, String absPath, InputStream istream, String location) {
        try {
            String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            if (session instanceof HippoSession) {
                ((HippoSession) session).importDereferencedXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                        ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
            } else {
                session.importXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            }
        } catch (IOException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        } catch (PathNotFoundException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        } catch (ItemExistsException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                if(!ex.getMessage().startsWith("Node with the same UUID exists:") || log.isDebugEnabled()) {
                    log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
                }
            }
        } catch (ConstraintViolationException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        } catch (VersionException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        } catch (InvalidSerializedDataException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                if(!ex.getMessage().startsWith("Node with the same UUID exists:") || log.isDebugEnabled()) {
                    log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
                }
            }
        } catch (LockException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } else {
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}
