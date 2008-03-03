/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.servicing.ServicingDecoratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LocalHippoRepository extends HippoRepositoryImpl {
    /** SVN id placeholder */
    private final static String SVN_ID = "$Id$";

    /** Hippo Namespace */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    /** Hippo Namespace prefix */
    public final static String NAMESPACE_PREFIX = "hippo";

    /** System property for overriding the repostiory path */
    public final static String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public final static String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    protected final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    private JackrabbitRepository jackrabbitRepository = null;
    private ServicingDecoratorFactory hippoRepositoryFactory;

    /** Whether to generate a dump.xml file of the /hippo:configuration node at shutdown */
    private boolean dump = false;

    /** Listener for changes under /hippo:configuration/hippo:initialize node */
    private EventListener listener;

    public LocalHippoRepository() throws RepositoryException {
        super();
        initialize();
    }

    public LocalHippoRepository(String location) throws RepositoryException {
        super(location);
        initialize();
    }

    public static HippoRepository create(String location) throws RepositoryException {
        if(location == null)
            return new LocalHippoRepository();
        else
            return new LocalHippoRepository(location);
    }

    public String getLocation() {
        return super.getLocation();
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Construct the repository path, default getWorkingDirectory() is used.
     * If the system property repo.path can be used to override the default.
     * If repo.path starts with a '.' then the path is taken relative to the
     * getWorkingDirectory().
     * @return The absolute path to the file repository
     */
    private String getRepositoryPath() {
        String path = System.getProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY);

        if (path == null || "".equals(path)) {
            path = getWorkingDirectory();
        } else if (path.charAt(0) == '.') {
            // relative path
            path = getWorkingDirectory() + System.getProperty("file.separator") + path;
        } else if (path.startsWith("file://")) {
            path = path.substring(6);
        } else if (path.startsWith("file:/")) {
            path = path.substring(5);
        } else if (path.startsWith("file:")) {
            path = "/" + path.substring(5);
        }
        log.info("Using repository path: " + path);
        return path;
    }

    /**
     * If the "file://" protocol is used, the path MUST be absolute.
     * In all other cases the config file is used as a class resource.
     * @return InputStream to the repository config
     * @throws RepositoryException
     */
    private InputStream getRepositoryConfigAsStream() throws RepositoryException {
        // get config from system prop
        String configName = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        // if not set try to use the servletconfig
        if (configName == null || "".equals(configName)) {
            configName = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        // if still not set use default
        if (configName == null || "".equals(configName)) {
            log.info("Using default repository config: " + DEFAULT_REPOSITORY_CONFIG);
            return getClass().getResourceAsStream(DEFAULT_REPOSITORY_CONFIG);
        }

        // resource
        if (!configName.startsWith("file:")) {
            log.info("Using resource repository config: " + configName);
            return getClass().getResourceAsStream(configName);
        }

        // parse file name
        if (configName.startsWith("file://")) {
            configName = configName.substring(6);
        } else if (configName.startsWith("file:/")) {
            configName = configName.substring(5);
        } else if (configName.startsWith("file:")) {
            configName = "/" + configName.substring(5);
        }
        log.info("Using file repository config: file:/" + configName);

        // get the bufferedinputstream
        File configFile = new File(configName);
        try {
            FileInputStream fis = new FileInputStream(configFile);
            return new BufferedInputStream(fis);
        } catch (FileNotFoundException e) {
            throw new RepositoryException("Repository config not found: file:/" + configName);
        }
    }

    private void initialize() throws RepositoryException {
        jackrabbitRepository = RepositoryImpl.create(RepositoryConfig.create(getRepositoryConfigAsStream(),
                getRepositoryPath()));
        repository = jackrabbitRepository;

        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));

        hippoRepositoryFactory = new ServicingDecoratorFactory();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);

        try {
            // get the current root/system session for the default workspace
            Session rootSession =  ((RepositoryImpl)jackrabbitRepository).getRootSession(null);
            Workspace workspace = rootSession.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

            try {
                initializeNamespace(nsreg, NAMESPACE_PREFIX, NAMESPACE_URI);
            } catch (UnsupportedRepositoryOperationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            }

            try {
                String cndName = "repository.cnd";
                log.info("Initializing nodetypes from: " + cndName);
                initializeNodetypes(workspace, getClass().getResourceAsStream(cndName), cndName);
                rootSession.save();
            } catch (ConstraintViolationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (InvalidItemStateException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ItemExistsException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (LockException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (NoSuchNodeTypeException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ParseException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (VersionException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            }

            if (!rootSession.getRootNode().hasNode("hippo:configuration")) {
                log.info("Initializing configuration content");
                try {
                    InputStream configuration = getClass().getResourceAsStream("configuration.xml");
                    if (configuration != null) {
                        initializeNodecontent(rootSession, "/", configuration);
                        rootSession.save();
                    }
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
            } else {
                log.info("Initial configuration content already present");
            }
            try {
                List extensions = new LinkedList();
                for(Enumeration iter = getClass().getClassLoader().getResources("org/hippoecm/repository/extension.xml");
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
                 *         extensions.addpend(configurationURL);
                 *     }
                 */
                for(Iterator iter = extensions.iterator(); iter.hasNext(); ) {
                    URL configurationURL = (URL) iter.next();
                    log.info("Initializing additional configuration content from "+configurationURL);
                    try {
                        InputStream configurationStream = configurationURL.openStream();
                        initializeNodecontent(rootSession, "/hippo:configuration/hippo:temporary", configurationStream);
                        Node mergeInitializationNode = rootSession.getRootNode().
                            getNode("hippo:configuration/hippo:temporary/hippo:initialize");
                        for(NodeIterator mergeIter = mergeInitializationNode.getNodes(); mergeIter.hasNext(); ) {
                            Node n = mergeIter.nextNode();
                            if(!rootSession.getRootNode().hasNode("hippo:configuration/hippo:initialize/" +
                                                                  n.getName())) {
                                rootSession.move(n.getPath(), "/hippo:configuration/hippo:initialize/" + n.getName());
                            } else {
                                log.warn("Node " + n.getName() + " already exists in initialize folder (source: " + configurationURL.toString() + ")");
                            }
                        }
                        mergeInitializationNode.remove();
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
            } catch (IOException ex) {
                throw new RepositoryException("Could not obtain initial configuration from classpath", ex);
            }

            refresh();

            /* Register a listener for the initialize node.  Whenever a node
             * or property is added, refresh the tree.  Processed properties
             * are deleted, so they will not be processed more than once.
             */
            ObservationManager obMgr = workspace.getObservationManager();
            listener = new EventListener() {
                public void onEvent(EventIterator events) {
                    refresh();
                }
            };
            obMgr.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED, "/hippo:configuration/hippo:initialize",
                    true, null, null, true);

        } catch (LoginException ex) {
            log.error("no access to repository by repository itself", ex);
        }
    }

    private void refresh() {
        try {
            Session rootSession =  ((RepositoryImpl)jackrabbitRepository).getRootSession(null);
            Workspace workspace = rootSession.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

            Node configurationNode = rootSession.getRootNode().getNode("hippo:configuration");
            if (configurationNode.hasNode("hippo:initialize")) {
                Node initializationNode = null;
                try {
                    initializationNode = configurationNode.getNode("hippo:initialize");
                } catch (PathNotFoundException ex) {
                    assert (initializationNode != null); // cannot happen
                }
                log.info("Looking for custom initializations at " + initializationNode.getPath());

                /* This orderes the list of nodes, according to a hippo:sequence property */
                double highest = 0.0;
                for (NodeIterator iter = initializationNode.getNodes(); iter.hasNext();) {
                    Node node = iter.nextNode();
                    if (node.hasProperty(HippoNodeType.HIPPO_SEQUENCE)) {
                        double value = node.getProperty(HippoNodeType.HIPPO_SEQUENCE).getDouble();
                        if(value > highest) {
                            highest = value;
                        }
                    }
                }
                SortedMap<Double,List<String>> ordered = new TreeMap<Double,List<String>>();
                for (NodeIterator iter = initializationNode.getNodes(); iter.hasNext();) {
                    Node node = iter.nextNode();
                    Double value;
                    if (node.hasProperty(HippoNodeType.HIPPO_SEQUENCE)) {
                        value = new Double(- node.getProperty(HippoNodeType.HIPPO_SEQUENCE).getDouble());
                    } else {
                        value = new Double(- highest);
                        highest += 1.0;
                    }
                    
                    List<String> siblings;
                    if(ordered.containsKey(value)) {
                        siblings = ordered.get(value);
                    } else {
                        siblings = new LinkedList<String>();
                        ordered.put(value, siblings);
                    }
                    siblings.add(node.getName());
                }
                String previous = null;
                for (Iterator<List<String>> iter = ordered.values().iterator(); iter.hasNext(); ) {
                    for (String current : iter.next()) {
                        initializationNode.orderBefore(current, previous);
                        previous = current;
                    }
                }
                initializationNode.save();

                for (NodeIterator iter = initializationNode.getNodes(); iter.hasNext();) {
                    Node node = iter.nextNode();
                    log.info("Initializing configuration from " + node.getName());
                    try {
                        if (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                            if (log.isDebugEnabled())
                                log.debug("Found namespace configuration");
                            Property p = null;
                            try {
                                String namespace = (p = node.getProperty(HippoNodeType.HIPPO_NAMESPACE)).getString();
                                log.info("Initializing namespace: " + node.getName() + " " + namespace);
                                // Add namespace if it doesn't exist
                                initializeNamespace(nsreg, node.getName(), namespace);
                                p.remove();
                            } catch (PathNotFoundException ex) {
                                assert (p != null); // cannot happen
                            }
                        }
                        if (node.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE) ||
                            node.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                            if (log.isDebugEnabled())
                                log.debug("Found nodetypes configuration");
                            if (node.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE) &&
                                node.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                                log.error("Initialize cannot contain both " + HippoNodeType.HIPPO_NODETYPESRESOURCE + " and " +
                                          HippoNodeType.HIPPO_NODETYPES + " definition");
                            }
                            Property p = null;
                            try {
                                String cndName;
                                InputStream cndStream;
                                if(node.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                                    cndName = (p = node.getProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)).getString();
                                    cndStream = getClass().getResourceAsStream(cndName);
                                } else {
                                    cndName = "<<internal>>";
                                    cndStream = (p = node.getProperty(HippoNodeType.HIPPO_NODETYPES)).getStream();
                                }
                                if (cndStream == null) {
                                    log.warn("Cannot locate nodetype configuration '" + cndName + "', initialization skipped");
                                } else {
                                    log.info("Initializing nodetypes from: " + cndName);
                                    initializeNodetypes(workspace, cndStream, cndName);
                                    p.remove();
                                }
                            } catch (PathNotFoundException ex) {
                                assert (p != null); // cannot happen
                            }
                        }
                        if (node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) ||
                            node.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                            if (log.isDebugEnabled())
                                log.debug("Found content configuration");
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) &&
                                node.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                                log.error("Initialize cannot contain both " + HippoNodeType.HIPPO_CONTENTRESOURCE + " and " +
                                          HippoNodeType.HIPPO_CONTENT + " definition");
                            }
                            Property contentProperty = null;
                            Property rootProperty = null;
                            try {
                                String contentName;
                                InputStream contentStream;
                                if(node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)) {
                                    contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE);
                                    contentName = contentProperty.getString();
                                    contentStream = getClass().getResourceAsStream(contentName);
                                } else {
                                    contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENT);
                                    contentName = "<<internal>>";
                                    contentStream = contentProperty.getStream();
                                }
                                if (contentStream == null) {
                                    log.warn("Cannot locate content configuration '" + contentName + "', initialization skipped");
                                } else {
                                    String root = "/";
                                    if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                        root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                                        rootProperty.remove();
                                    }

                                    // verify that content root is not under the initialization node
                                    String initPath = initializationNode.getPath();
                                    if (root.length() > initPath.length()
                                            && root.substring(0, initPath.length()) == initPath) {
                                        log.error("Refusing to extract content to " + root);
                                    } else {
                                        log.info("Initializing content from: " + contentName + " to " + root);
                                        initializeNodecontent(rootSession, root, contentStream);
                                    }
                                    contentProperty.remove();
                                }
                            } catch (PathNotFoundException ex) {
                                assert (contentProperty != null); // cannot happen
                                assert (rootProperty != null); // cannot happen
                            }
                        }
                        rootSession.save();
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
                    } finally {
                        rootSession.refresh(false);
}
                }
            }
        } catch (PathNotFoundException ex) {
            log.error("configuration node still not present");
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void initializeNamespace(NamespaceRegistry nsreg, String prefix, String uri)
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
                    log.error("Prefix already used for different namespace");
                    return;
                }
                String newPrefix = prefix + "_" + currentURI.substring(uriPrefix.length());
                ((NamespaceRegistryImpl)nsreg).externalRemap(prefix, newPrefix, currentURI);
            } catch (NamespaceException ex) {
                if (!ex.getMessage().endsWith("is not a registered namespace prefix.")) {
                    log.warn(ex.getMessage());
                }
            }

            nsreg.registerNamespace(prefix, uri);

        } catch (NamespaceException ex) {
            if (ex.getMessage().endsWith("mapping already exists")) {
                log.error("Namespace already exists: " + prefix + ":" + uri);
            } else {
                log.warn(ex.getMessage());
            }
        }
    }

    private void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws ParseException,
            RepositoryException {
        CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName);
        List ntdList = cndReader.getNodeTypeDefs();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();

            try {
                ntreg.unregisterNodeType(ntd.getName());
            } catch (NoSuchNodeTypeException ex) {
                // new type, ignore
            } catch (RepositoryException ex) {
                // kind of safe to ignore
            }

            try {
                EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
                log.info("Added NodeType: " + ntd.getName().getLocalName());
            } catch (NamespaceException ex) {
                log.warn(ex.getMessage());
            } catch (InvalidNodeTypeDefException ex) {
                if (ex.getMessage().endsWith("already exists")) {
                    log.debug(ex.getMessage());
                } else {
                    log.warn(ex.getMessage());
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    log.warn(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initializeNodecontent(Session session, String absPath, InputStream istream) {
        try {
            String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            session.importXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (PathNotFoundException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (ItemExistsException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (ConstraintViolationException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (VersionException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (InvalidSerializedDataException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (LockException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public synchronized void close() {
        Session session = null;
        if (dump && repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/hippo:configuration", out, false, false);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        if (listener != null) {
            try {
                Session rootSession =  ((RepositoryImpl)jackrabbitRepository).getRootSession(null);
                Workspace workspace = rootSession.getWorkspace();
                ObservationManager obMgr = workspace.getObservationManager();
                obMgr.removeEventListener(listener);
            } catch(Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            listener = null;
        }

        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                // ignore
            }
        }
        repository = null;

        super.close();
    }
}
