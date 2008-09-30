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
import javax.jcr.SimpleCredentials;
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
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LocalHippoRepository extends HippoRepositoryImpl {
    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** Hippo Namespace */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    /** Hippo Namespace prefix */
    public final static String NAMESPACE_PREFIX = "hippo";

    /** System property for overriding the repository path */
    public final static String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repository config file */
    public final static String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for overriding the servlet config file */
    public final static String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public final static String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    /** hippo decorated root session */
    private HippoSession rootSession;
    
    protected final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    private JackrabbitRepository jackrabbitRepository = null;
    private DecoratorFactoryImpl hippoRepositoryFactory;

    /** Whether to generate a dump.xml file of the /hippo:configuration node at shutdown */
    private final boolean dump = false;

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

    @Override
    public String getLocation() {
        return super.getLocation();
    }

    @Override
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

    private class LocalRepositoryImpl extends RepositoryImpl {
        LocalRepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
            super(repConfig);
        }
        @Override
        protected Session getRootSession(String workspaceName) throws RepositoryException {
            return super.getRootSession(workspaceName);
        }
    }

    private void initialize() throws RepositoryException {

        jackrabbitRepository = new LocalRepositoryImpl(RepositoryConfig.create(getRepositoryConfigAsStream(),
                getRepositoryPath()));
        repository = jackrabbitRepository;

        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));

        hippoRepositoryFactory = new DecoratorFactoryImpl();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);

        try {
            // get the current root/system session for the default workspace for namespace and nodetypes init
            Session jcrRootSession =  ((LocalRepositoryImpl)jackrabbitRepository).getRootSession(null);
            
            if(!jcrRootSession.getRootNode().isNodeType("mix:referenceable")) {
                jcrRootSession.getRootNode().addMixin("mix:referenceable");
                jcrRootSession.save();
            }

            try {
                log.info("Initializing hippo namespace");
                initializeNamespace(jcrRootSession.getWorkspace().getNamespaceRegistry(), NAMESPACE_PREFIX, NAMESPACE_URI);
            } catch (UnsupportedRepositoryOperationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            }

            try {
                String cndName = "repository.cnd";
                log.info("Initializing nodetypes from: " + cndName);
                initializeNodetypes(jcrRootSession.getWorkspace(), getClass().getResourceAsStream(cndName), cndName);
                jcrRootSession.save();
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

            // After initializing namespaces and nodetypes switch to the decorated session.
            rootSession = (HippoSession) hippoRepositoryFactory.getSessionDecorator(repository, jcrRootSession.impersonate(new SimpleCredentials("system", new char[]{})));
            
            
            if (!rootSession.getRootNode().hasNode("hippo:configuration")) {
                log.info("Initializing configuration content");
                InputStream configuration = getClass().getResourceAsStream("configuration.xml");
                if (configuration != null) {
                    initializeNodecontent(rootSession, "/", configuration, getClass().getPackage().getName() + ".configuration.xml");
                } else {
                    log.error("Could not initialize configuration content: ResourceAsStream not found: configuration.xml");
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
                for(Enumeration iter = getClass().getClassLoader().getResources("hippoecm-extension.xml");
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
                 * TODO: Use merge behavior from dereferenced import? [BvdS]    
                 */
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
                            if("hippoecm-extension.xml".equals(configurationURL.getFile().contains("/")
                                           ? configurationURL.getFile().substring(configurationURL.getFile().lastIndexOf("/")+1)
                                           : configurationURL.getFile())) {
                                n.setProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE, configurationURL.toString());
                            }
                            if(!rootSession.getRootNode().hasNode("hippo:configuration/hippo:initialize/" +
                                                                  n.getName())) {
                                rootSession.move(n.getPath(), "/hippo:configuration/hippo:initialize/" + n.getName());
                            } else {
                                log.info("Node " + n.getName() + " already exists in initialize folder (source: " + configurationURL.toString() + ")");
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
            ObservationManager obMgr = rootSession.getWorkspace().getObservationManager();
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

    /**
     * TODO: Needs refactoring! Move to separate class
     */
    private synchronized void refresh() {
        try {
            Workspace workspace = rootSession.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
            
            
            Node configurationNode = rootSession.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
            if (configurationNode.hasNode(HippoNodeType.INITIALIZE_PATH)) {
                Node initializationNode = null;
                try {
                    initializationNode = configurationNode.getNode(HippoNodeType.INITIALIZE_PATH);
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
                        
                        // Namespace
                        if (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Found namespace configuration");
                            }
                            Property p = node.getProperty(HippoNodeType.HIPPO_NAMESPACE);
                            String namespace = p.getString();
                            log.info("Initializing namespace: " + node.getName() + " " + namespace);
                            // Add namespace if it doesn't exist
                            initializeNamespace(nsreg, node.getName(), namespace);
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
                                if(node.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                                    URL resource = new URL(node.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE).getString());
                                    resource = new URL(resource, cndName);
                                    cndName = resource.toString();
                                    cndStream = resource.openStream();
                                } else {
                                    cndStream = getClass().getResourceAsStream(cndName);
                                }
                            }
                            if (cndStream == null) {
                                log.error("Cannot locate nodetype configuration '" + cndName + "', initialization skipped");
                            } else {
                                log.info("Initializing nodetypes from: " + cndName);
                                initializeNodetypes(workspace, cndStream, cndName);
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
                                initializeNodetypes(workspace, cndStream, cndName);
                                p.remove();
                            }
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
                                if(node.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                                    URL resource = new URL(node.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE).getString());
                                    resource = new URL(resource, contentName);
                                    contentName = resource.toString();
                                    contentStream = resource.openStream();
                                } else {
                                    contentStream = getClass().getResourceAsStream(contentName);    
                                }
                            }
                                
                            if (contentStream == null) {
                                log.error("Cannot locate content configuration '" + contentName + "', initialization skipped");
                            } else {
                                String root = "/";
                                Property rootProperty = null;
                                if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                    root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                                }

                                // verify that content root is not under the initialization node
                                String initPath = initializationNode.getPath();
                                if (root.length() > initPath.length()
                                        && root.substring(0, initPath.length()) == initPath) {
                                    log.error("Refusing to extract content to " + root);
                                } else {
                                    log.info("Initializing content from: " + contentName + " to " + root);
                                    initializeNodecontent(rootSession, root, contentStream, contentName);
                                }
                                rootProperty.remove();
                                contentProperty.remove();
                            }
                        }
                        

                        // CONTENT FROM NODE
                        if (node.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Found content resource configuration");
                            }
                            
                            Property contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENT);
                            String contentName = "<<internal>>";
                            InputStream contentStream = contentProperty.getStream();
                                
                            if (contentStream == null) {
                                log.error("Cannot locate content configuration '" + contentName + "', initialization skipped");
                            } else {
                                String root = "/";
                                Property rootProperty = null;
                                if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                    root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                                }

                                // verify that content root is not under the initialization node
                                String initPath = initializationNode.getPath();
                                if (root.length() > initPath.length()
                                        && root.substring(0, initPath.length()) == initPath) {
                                    log.error("Refusing to extract content to " + root);
                                } else {
                                    log.info("Initializing content from: " + contentName + " to " + root);
                                    initializeNodecontent(rootSession, root, contentStream, contentName+":"+node.getPath());
                                }
                                rootProperty.remove();
                                contentProperty.remove();
                            }
                        }
                        rootSession.save();
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
                    log.error("Prefix already used for different namespace: " + prefix + ":" + uri);
                    return;
                }
                String newPrefix = prefix + "_" + currentURI.substring(uriPrefix.length());
                ((NamespaceRegistryImpl)nsreg).externalRemap(prefix, newPrefix, currentURI);
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
                log.error(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName());
            } catch (InvalidNodeTypeDefException ex) {
                if (ex.getMessage().endsWith("already exists")) {
                    log.info(ex.getMessage() +". In " + cndName +" for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName());
                } else {
                    log.error(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName());
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    log.warn(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initializeNodecontent(Session session, String absPath, InputStream istream, String location) {
        try {
            String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            ((HippoSession) session).importDereferencedXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
            session.save();
        } catch (IOException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (PathNotFoundException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (ItemExistsException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (ConstraintViolationException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (VersionException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (InvalidSerializedDataException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (LockException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
        } finally {
            try {
                session.refresh(false);
            } catch (RepositoryException ex) {
                log.error("Error refreshing session while initilizing content for "+location+" in '" + absPath + "' : " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public synchronized void close() {
        Session session = null;
        if (dump && repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/hippo:configuration", out, false, false);
            } catch (IOException ex) {
                log.error("Error while dumping comfiguration: " + ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                log.error("Error while dumping comfiguration: " + ex.getMessage(), ex);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        if (listener != null) {
            try {
                Session rootSession =  ((LocalRepositoryImpl)jackrabbitRepository).getRootSession(null);
                Workspace workspace = rootSession.getWorkspace();
                ObservationManager obMgr = workspace.getObservationManager();
                obMgr.removeEventListener(listener);
            } catch (Exception ex) {
                log.error("Error while removing listener: " + ex.getMessage(), ex);
            }
            listener = null;
        }

        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                log.error("Error while shuting down jackrabbitRepository: " + ex.getMessage(), ex);
            }
        }
        repository = null;

        super.close();
    }
}
