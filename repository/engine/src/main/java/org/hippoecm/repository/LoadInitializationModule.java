/*
 *  Copyright 2010-2012 Hippo.
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
import java.util.HashSet;
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
import org.hippoecm.repository.util.MavenComparableVersion;
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
        "WHERE jcr:path = '/hippo:configuration/hippo:initialize/%' AND " +
        HippoNodeType.HIPPO_STATUS + " = 'pending'" +
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
                    "SELECT * FROM hipposys:initializeitem " +
                    "WHERE jcr:path = '/initialize/%' AND " +
                    HippoNodeType.HIPPO_STATUS + " = 'pending'" +
                    "ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC", Query.SQL);
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
                final Node node = initializeItems.nextNode();
                if (!node.hasProperty(HippoNodeType.HIPPO_STATUS) || !node.getProperty(HippoNodeType.HIPPO_STATUS).getString().equals("pending")) {
                    continue;
                }
                log.info("Initializing configuration from " + node.getName());
                try {
                    // Namespace
                    if (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found namespace configuration");
                        }
                        String namespace = node.getProperty(HippoNodeType.HIPPO_NAMESPACE).getString();
                        log.info("Initializing namespace: " + node.getName() + " " + namespace);
                        // Add namespace if it doesn't exist
                        if (!dryRun) {
                            initializeNamespace(nsreg, node.getName(), namespace);
                        }
                    }

                    // Nodetypes from FILE
                    if (node.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found nodetypes resource configuration");
                        }
                        String cndResource = node.getProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE).getString();
                        InputStream cndStream = getResourceStream(node, cndResource);
                        if (cndStream == null) {
                            log.error("Cannot locate nodetype configuration '" + cndResource + "', initialization skipped");
                        } else {
                            log.info("Initializing nodetypes from: " + cndResource);
                            if (!dryRun) {
                                initializeNodetypes(workspace, cndStream, cndResource);
                            }
                        }
                    }

                    // Nodetypes from node
                    if (node.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found nodetypes configuration");
                        }
                        String cndName = "<<internal>>";
                        InputStream cndStream = node.getProperty(HippoNodeType.HIPPO_NODETYPES).getStream();
                        if (cndStream == null) {
                            log.error("Cannot get stream for nodetypes definition property.");
                        } else {
                            log.info("Initializing nodetypes from nodetypes property.");
                            if (!dryRun) {
                                initializeNodetypes(workspace, cndStream, cndName);
                            }
                        }
                    }

                    // Delete content
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTDELETE)) {
                        String path = node.getProperty(HippoNodeType.HIPPO_CONTENTDELETE).getString();
                        log.warn("Delete content in initialization has been deprecrated: " + node.getName() + " " + path);
                        boolean immediateSave;
                        if(node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) || node.hasProperty(HippoNodeType.HIPPO_CONTENT))
                            immediateSave = false;
                        else
                            immediateSave = true;
                        log.info("Delete content in initialization: " + node.getName() + " " + path);
                        removeNodecontent(session, path, immediateSave && !dryRun);
                    }

                    // Content from file
                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Found content resource configuration");
                        }

                        String contentResource = node.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE).getString();
                        InputStream contentStream = getResourceStream(node, contentResource);

                        if (contentStream == null) {
                            log.error("Cannot locate content configuration '" + contentResource
                                    + "', initialization skipped");
                        } else {
                            String root = "/";
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                root = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT).getString();
                            }

                            // verify that content root is not under the initialization node
                            String initPath = initializationNode.getPath();
                            if (root.startsWith(initPath)) {
                                log.error("Refusing to extract content to " + root);
                            } else {
                                if (node.hasProperty(HippoNodeType.HIPPO_RELOADONSTARTUP) && node.getProperty(HippoNodeType.HIPPO_RELOADONSTARTUP).getBoolean()) {
                                    if (node.hasProperty(HippoNodeType.HIPPO_CONTEXTNODENAME)) {
                                        String contextNodeName = node.getProperty(HippoNodeType.HIPPO_CONTEXTNODENAME).getString();
                                        String contextNodePath = root.equals("/") ? root + contextNodeName : root + "/" + contextNodeName;
                                        removeNodecontent(session, contextNodePath, false);
                                        log.info("Initializing content from: " + contentResource + " to " + root);
                                        initializeNodecontent(session, root, contentStream, contentResource);
                                    } else {
                                        log.warn("Cannot remove node for reloading content");
                                    }
                                } else {
                                    log.info("Initializing content from: " + contentResource + " to " + root);
                                    initializeNodecontent(session, root, contentStream, contentResource);
                                }
                            }
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
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                root = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT).getString();
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
                        if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                            root = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT).getString();
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
                            Set<NodeType> nodeTypes = new HashSet<NodeType>();
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
                        node.setProperty(HippoNodeType.HIPPO_STATUS, "done");
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
                } finally {
                    session.refresh(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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
                final String moduleVersion = getModuleVersion(configurationURL);
                for(NodeIterator mergeIter = mergeInitializationNode.getNodes(); mergeIter.hasNext(); ) {
                    Node n = mergeIter.nextNode();

                    final String existingModuleVersion = getExistingModuleVersion(initializationFolder, n.getName());

                    if (!initializationFolder.hasNode(n.getName()) || shouldReload(n, initializationFolder.getNode(n.getName()), moduleVersion, existingModuleVersion)) {

                        if(initializationFolder.hasNode(n.getName())) {
                            // this occurs when reload is on
                            log.info("Item " + n.getName() + " needs to be reloaded");
                            initializationFolder.getNode(n.getName()).remove();
                        }
                        Node moved = initializationFolder.addNode(n.getName(), HippoNodeType.NT_INITIALIZEITEM);
                        if("hippoecm-extension.xml".equals(configurationURL.getFile().contains("/")
                                       ? configurationURL.getFile().substring(configurationURL.getFile().lastIndexOf("/")+1)
                                       : configurationURL.getFile())) {
                            moved.setProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE, configurationURL.toString());
                            if (moduleVersion != null) {
                                moved.setProperty(HippoNodeType.HIPPO_EXTENSIONVERSION, moduleVersion);
                            }
                        }
                        for (String propertyName : new String[] { HippoNodeType.HIPPO_SEQUENCE, HippoNodeType.HIPPO_NAMESPACE, HippoNodeType.HIPPO_NODETYPESRESOURCE, HippoNodeType.HIPPO_NODETYPES, HippoNodeType.HIPPO_CONTENTRESOURCE, HippoNodeType.HIPPO_CONTENT, HippoNodeType.HIPPO_CONTENTROOT, HippoNodeType.HIPPO_CONTENTDELETE, HippoNodeType.HIPPO_CONTENTPROPSET, HippoNodeType.HIPPO_CONTENTPROPADD, HippoNodeType.HIPPO_RELOADONSTARTUP }) {
                            if(n.hasProperty(propertyName)) {
                                final Property property = n.getProperty(propertyName);
                                if(property.getDefinition().isMultiple()) {
                                    moved.setProperty(propertyName, property.getValues(), property.getType());
                                } else {
                                    moved.setProperty(propertyName, property.getValue());
                                }
                            }
                        }
                        String contextNodeName = null;
                        if (moved.hasProperty(HippoNodeType.HIPPO_RELOADONSTARTUP) && moved.getProperty(HippoNodeType.HIPPO_RELOADONSTARTUP).getBoolean() && moved.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)) {
                            contextNodeName = readContextNodeName(moved);
                        }
                        if (contextNodeName != null) {
                            moved.setProperty(HippoNodeType.HIPPO_CONTEXTNODENAME, contextNodeName);
                            String root = "/";
                            if (moved.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                root = moved.getProperty(HippoNodeType.HIPPO_CONTENTROOT).getString();
                            }
                            String contextNodePath = root.equals("/") ? root + contextNodeName : root + "/" + contextNodeName;
                            final NodeIterator downstreamItems = getDownstreamItems(rootSession, contextNodePath);
                            while (downstreamItems.hasNext()) {
                                final Node node = downstreamItems.nextNode();
                                log.info("Marking downstream item " + node.getName() + " for reload");
                                node.setProperty(HippoNodeType.HIPPO_STATUS, "pending");
                            }
                        }
                        moved.setProperty(HippoNodeType.HIPPO_STATUS, "pending");
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

    private static boolean shouldReload(final Node temp, final Node existing, final String moduleVersion, final String existingModuleVersion) throws RepositoryException {
        if (!temp.hasProperty(HippoNodeType.HIPPO_RELOADONSTARTUP) || !temp.getProperty(HippoNodeType.HIPPO_RELOADONSTARTUP).getBoolean()) {
            return false;
        }
        if (!isNewerVersion(moduleVersion, existingModuleVersion)) {
            return false;
        }
        if (existing.hasProperty(HippoNodeType.HIPPO_STATUS) && existing.getProperty(HippoNodeType.HIPPO_STATUS).getString().equals("disabled")) {
            return false;
        }
        return true;
    }

    private static boolean isNewerVersion(final String moduleVersion, final String existingModuleVersion) {
        if (moduleVersion == null) {
            return false;
        }
        if (existingModuleVersion == null) {
            return true;
        }
        try {
            return new MavenComparableVersion(moduleVersion).compareTo(new MavenComparableVersion(existingModuleVersion)) > 0;
        } catch (RuntimeException e) {
            // version could not be parsed
            log.error("Invalid module version: " + moduleVersion + " or existing: " + existingModuleVersion);
        }
        return false;
    }

    private static String getModuleVersion(URL configurationURL) {
        String configurationURLString = configurationURL.toString();
        if (configurationURLString.endsWith("hippoecm-extension.xml")) {
            String manifestUrlString = configurationURLString.substring(0, configurationURLString.length() - "hippoecm-extension.xml".length()) + "META-INF/MANIFEST.MF";
            try {
                Manifest manifest = new Manifest(new URL(manifestUrlString).openStream());
                return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Build"));
            } catch (IOException ex) {
                // deliberate ignore, manifest file not available so no build number can be obtained
            }
        }
        return null;
    }

    private static String getExistingModuleVersion(final Node initializationFolder, final String initializeItemName) throws RepositoryException {
        if (initializationFolder.hasNode(initializeItemName)) {
            final Node initializeItemNode = initializationFolder.getNode(initializeItemName);
            if (initializeItemNode.hasProperty(HippoNodeType.HIPPO_EXTENSIONVERSION)) {
                return initializeItemNode.getProperty(HippoNodeType.HIPPO_EXTENSIONVERSION).getString();
            }
            if (initializeItemNode.hasProperty(HippoNodeType.HIPPO_EXTENSIONBUILD)) {
                return Long.valueOf(initializeItemNode.getProperty(HippoNodeType.HIPPO_EXTENSIONBUILD).getLong()).toString();
            }
        }
        return null;
    }

    private static NodeIterator getDownstreamItems(final Session session, final String contextNodePath) throws RepositoryException {
        return session.getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM hipposys:initializeitem WHERE " +
                "jcr:path = '/hippo:configuration/hippo:initialize/%' AND (" +
                HippoNodeType.HIPPO_CONTENTROOT + " = '" + contextNodePath + "' OR " +
                HippoNodeType.HIPPO_CONTENTROOT + " LIKE '" + contextNodePath + "/%')", Query.SQL
        ).execute().getNodes();
    }

    private static String readContextNodeName(final Node item) {
        if (factory == null) {
            return null;
        }
        try {
            InputStream contentStream = getResourceStream(item, item.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE).getString());
            if (contentStream != null) {
                try {
                    // inspect the xml file to find out if it is a delta xml and to read the name of the context node we must remove
                    boolean removeSupported = true;
                    String contextNodeName = null;
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
                    if (removeSupported) {
                        return contextNodeName;
                    }
                } finally {
                    try { contentStream.close(); } catch (IOException ignore) {}
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not read root node name from content file", e);
        } catch (XmlPullParserException e) {
            log.error("Could not read root node name from content file", e);
        } catch (IOException e) {
            log.error("Could not read root node name from content file", e);
        }
        return null;
    }

    private static InputStream getResourceStream(final Node item, String resourcePath) throws RepositoryException, IOException {
        InputStream resourceStream = null;
        if (resourcePath.startsWith("file:")) {
            if (resourcePath.startsWith("file://")) {
                resourcePath = resourcePath.substring(6);
            } else if (resourcePath.startsWith("file:/")) {
                resourcePath = resourcePath.substring(5);
            } else if (resourcePath.startsWith("file:")) {
                resourcePath = "/" + resourcePath.substring(5);
            }
            File localFile = new File(resourcePath);
            try {
                resourceStream = new BufferedInputStream(new FileInputStream(localFile));
            } catch (FileNotFoundException e) {
                log.error("Resource file not found: " + resourceStream, e);
            }
        } else {
            if (item.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                URL resource = new URL(item.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE).getString());
                resource = new URL(resource, resourcePath);
                resourceStream = resource.openStream();
            } else {
                resourceStream = LocalHippoRepository.class.getResourceAsStream(resourcePath);
            }
        }
        return resourceStream;
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
