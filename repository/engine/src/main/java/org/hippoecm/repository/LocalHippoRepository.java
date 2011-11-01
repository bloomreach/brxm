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
import java.net.URL;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.checker.Checker;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.decorating.checked.CheckedDecoratorFactory;
import org.hippoecm.repository.ext.DaemonModule;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.hippoecm.repository.jackrabbit.HippoSessionItemStateManager;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.SecurityManager;
import org.hippoecm.repository.updater.UpdaterEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalHippoRepository extends HippoRepositoryImpl {
    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** System property for overriding the repository path */
    public static final String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_UPGRADE_PROPERTY = "repo.upgrade";

    /** System property for overriding the servlet config file */
    public static final String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    /** The advised threshold on the number of modified nodes to hold in transient session state */
    public static int batchThreshold = 96;

    /** hippo decorated root session */
    private HippoSession rootSession;

    protected static final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    private LocalRepositoryImpl jackrabbitRepository = null;
    private DecoratorFactoryImpl hippoRepositoryFactory;

    /** Whether to generate a dump.xml file of the /hippo:configuration node at shutdown */
    private final boolean dump = false;

    /** Whether to perform an automatic upgrade from previous releases */
    private UpgradeFlag upgradeFlag = UpgradeFlag.TRUE;

    /** Whether to reindex after upgrading */
    private boolean upgradeReindexFlag = false;

    /** Whether to run a derived properties validation after upgrading */
    private boolean upgradeValidateFlag = true;

    public boolean stateThresholdExceeded(Session session, EnumSet<SessionStateThresholdEnum> interests) {
        session = org.hippoecm.repository.decorating.SessionDecorator.unwrap(session);
        session = org.hippoecm.repository.decorating.checked.SessionDecorator.unwrap(session);
        session = org.hippoecm.repository.impl.SessionDecorator.unwrap(session);
        if(session instanceof org.apache.jackrabbit.core.SessionImpl) {
            HippoSessionItemStateManager sessionISM = (HippoSessionItemStateManager) (session instanceof org.hippoecm.repository.jackrabbit.XASessionImpl ? ((org.hippoecm.repository.jackrabbit.XASessionImpl)session).getItemStateManager() : ((org.hippoecm.repository.jackrabbit.SessionImpl)session).getItemStateManager());
            return sessionISM.stateThresholdExceeded(interests);
        }
        return false;
    }

    private static enum UpgradeFlag {
        TRUE, FALSE, ABORT
    }

    /** When during startup a situation is detected that a restart is required, this flag signals this, but only one restart should be appropriate */
    boolean needsRestart = false;

    List<DaemonModule> daemonModules = new LinkedList<DaemonModule>();

    protected LocalHippoRepository() throws RepositoryException {
        super();
    }

    protected LocalHippoRepository(String location) throws RepositoryException {
        super(location);
    }

    public static HippoRepository create(String location) throws RepositoryException {
        LocalHippoRepository localHippoRepository;
        if(location == null)
            localHippoRepository = new LocalHippoRepository();
        else
            localHippoRepository = new LocalHippoRepository(location);
        localHippoRepository.initialize();
        VMHippoRepository.register(location, localHippoRepository);
        return localHippoRepository;
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
    private static InputStream getRepositoryConfigAsStream() throws RepositoryException {
        // get config from system prop
        String configName = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        // if not set try to use the servletconfig
        if (configName == null || "".equals(configName)) {
            configName = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        // if still not set use default
        if (configName == null || "".equals(configName)) {
            log.info("Using default repository config: " + DEFAULT_REPOSITORY_CONFIG);
            return LocalHippoRepository.class.getResourceAsStream(DEFAULT_REPOSITORY_CONFIG);
        }

        // resource
        if (!configName.startsWith("file:")) {
            log.info("Using resource repository config: " + configName);
            return LocalHippoRepository.class.getResourceAsStream(configName);
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
    
    public static boolean check(String location, boolean fix) throws RepositoryException {
        LocalHippoRepository repository= (location != null ? new LocalHippoRepository(location) : new LocalHippoRepository());
        RepositoryConfig repConfig = RepositoryConfig.create(repository.getRepositoryConfigAsStream(), repository.getRepositoryPath());
        Checker checker = new Checker(repConfig);
        return checker.check(fix);
    }

    public boolean check(boolean fix) throws RepositoryException {
        RepositoryConfig repConfig = RepositoryConfig.create(getRepositoryConfigAsStream(), getRepositoryPath());
        Checker checker = new Checker(repConfig);
        return checker.check(fix);
    }

    public static void check(String location, String[] arguments) throws RepositoryException {
        LocalHippoRepository repository= (location != null ? new LocalHippoRepository(location) : new LocalHippoRepository());
        RepositoryConfig repConfig = RepositoryConfig.create(repository.getRepositoryConfigAsStream(), repository.getRepositoryPath());
        Checker checker = new Checker(repConfig);
        checker.checkBundles(arguments);
    }

    private class LocalRepositoryImpl extends RepositoryImpl {
        LocalRepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
            super(repConfig);
        }
        @Override
        protected Session getRootSession(String workspaceName) throws RepositoryException {
            return super.getRootSession(workspaceName);
        }
        void enableVirtualLayer(boolean enabled) throws RepositoryException {
            isStarted = enabled;
        }
    }

    static private void delete(File path) {
        if(path.exists()) {
            if(path.isDirectory()) {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                    delete(files[i]);
            }
            if (!path.delete()) {
                log.warn("Unable to delete path: {}", path);
            }
        }
    }

    protected void initialize() throws RepositoryException {
        initializeStartup();
        if(needsRestart) {
            log.warn("restarting repository after upgrade cycle");
            close();
            if (upgradeReindexFlag) {
                log.warn("post migration cycle forced reindexing");
                initializeReindex();
            }
            initializeStartup();
            ((SecurityManager) jackrabbitRepository.getSecurityManager()).init();
            if (upgradeValidateFlag) {
                log.warn("post migration cycle validating content");
                ((org.hippoecm.repository.impl.SessionDecorator)rootSession).postValidation();
            }
        } else {
            ((SecurityManager) jackrabbitRepository.getSecurityManager()).init();
        }
    }

    private void initializeReindex() {
        File basedir = new File(getRepositoryPath());
        delete(new File(basedir, "repository/index"));
        delete(new File(basedir, "workspaces/default/index"));
    }

    private void initializeStartup() throws RepositoryException {

        Modules.setModules(new Modules(Thread.currentThread().getContextClassLoader()));

        jackrabbitRepository = new LocalRepositoryImpl(RepositoryConfig.create(getRepositoryConfigAsStream(),
                getRepositoryPath()));
        repository = jackrabbitRepository;

        String result = System.getProperty(SYSTEM_UPGRADE_PROPERTY);
        if (result != null) {
            for(String option : result.split(",")) {
                String key = "", value = "";
                if (option.contains("=")) {
                    String[] keyValue = option.split("=");
                    key = keyValue[0];
                    value = keyValue[1];
                }
                if (option.equalsIgnoreCase("abort")) {
                    upgradeFlag = UpgradeFlag.ABORT;
                } else if(key.equalsIgnoreCase("batchsize")) {
                   LocalHippoRepository.batchThreshold  = Integer.parseInt(value);
                } else if(option.equalsIgnoreCase("reindex")) {
                    upgradeReindexFlag = true;
                } else if(option.equalsIgnoreCase("validate")) {
                    upgradeValidateFlag = true;
                } else if(option.equalsIgnoreCase("skipreindex")) {
                    upgradeReindexFlag = false;
                } else if(option.equalsIgnoreCase("skipvalidate")) {
                    upgradeValidateFlag = false;
                } else if(option.equalsIgnoreCase("true")) {
                    upgradeFlag = UpgradeFlag.TRUE;
                } else if(option.equalsIgnoreCase("false")) {
                    upgradeFlag = UpgradeFlag.FALSE;
                } else {
                    log.warn("Unrecognized upgrade option \""+option+"\"");
                }
            }
        }
        switch(upgradeFlag) {
        case FALSE:
            log.info("Automatic upgrade enabled: false");
            break;
        case TRUE:
            log.info("Automatic upgrade enabled: true (reindexing "+(upgradeReindexFlag?"on":"off")+" revalidation "+(upgradeValidateFlag?"on":"off")+")");
            break;
        case ABORT:
            log.info("Automatic upgrade enabled: abort on upgrade required");
        }

        hippoRepositoryFactory = new DecoratorFactoryImpl();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);
        repository = new CheckedDecoratorFactory().getRepositoryDecorator(repository);

        try {
            // get the current root/system session for the default workspace for namespace and nodetypes init
            Session jcrRootSession =  ((LocalRepositoryImpl)jackrabbitRepository).getRootSession(null);

            if(!jcrRootSession.getRootNode().isNodeType("mix:referenceable")) {
                jcrRootSession.getRootNode().addMixin("mix:referenceable");
                jcrRootSession.save();
            }

            boolean hasHippoNamespace;
            try {
                jcrRootSession.getNamespaceURI("hippo");
                hasHippoNamespace = true;
            } catch (NamespaceException ex) {
                hasHippoNamespace = false;
            }

            if (hasHippoNamespace) {
                switch(upgradeFlag) {
                case TRUE:
                    ((LocalRepositoryImpl)jackrabbitRepository).enableVirtualLayer(false);
                    Session migrateSession = DecoratorFactoryImpl.getSessionDecorator(jcrRootSession.impersonate(new SimpleCredentials("system", new char[] {})));
                    needsRestart = UpdaterEngine.migrate(migrateSession);
                    migrateSession.logout();
                    if (needsRestart) {
                        return;
                    }
                    break;
                case FALSE:
                    break;
                case ABORT:
                    throw new RepositoryException("ABORT");
                }
            }

            Session syncSession = jcrRootSession.impersonate(new SimpleCredentials("system", new char[] {}));

            // TODO HREPTWO-3571: hippofacnav.cnd must be removed when faceted navigation is moved to its own subproject, and should be added through extension.xml, see
            
            for(String cndName : new String[] { "hippo.cnd", "hipposys.cnd", "hipposysedit.cnd", "hippofacnav.cnd" }) {
                try {
                    log.info("Initializing nodetypes from: " + cndName);
                    initializeNodetypes(syncSession.getWorkspace(), getClass().getClassLoader().getResourceAsStream(cndName), cndName);
                    syncSession.save();
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
            }

            ((LocalRepositoryImpl)jackrabbitRepository).enableVirtualLayer(true);

            // After initializing namespaces and nodetypes switch to the decorated session.
            rootSession = DecoratorFactoryImpl.getSessionDecorator(syncSession.impersonate(new SimpleCredentials("system", new char[]{})));

            if (!rootSession.getRootNode().hasNode("hippo:configuration")) {
                log.info("Initializing configuration content");
                InputStream configuration = getClass().getResourceAsStream("configuration.xml");
                if (configuration != null) {
                    initializeNodecontent(rootSession, "/", configuration, getClass().getPackage().getName() + ".configuration.xml");
                } else {
                    log.error("Could not initialize configuration content: ResourceAsStream not found: configuration.xml");
                }
                rootSession.save();
            } else {
                log.info("Initial configuration content already present");
            }
            try {
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
                            if(!rootSession.getRootNode().hasNode("hippo:configuration/hippo:initialize/" + n.getName())) {
                                Node moved = rootSession.getRootNode().getNode("hippo:configuration/hippo:initialize").addNode(n.getName(), HippoNodeType.NT_INITIALIZEITEM);
                                if("hippoecm-extension.xml".equals(configurationURL.getFile().contains("/")
                                               ? configurationURL.getFile().substring(configurationURL.getFile().lastIndexOf("/")+1)
                                               : configurationURL.getFile())) {
                                    moved.setProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE, configurationURL.toString());
                                }
                                for (String propertyName : new String[] { HippoNodeType.HIPPO_SEQUENCE, HippoNodeType.HIPPO_NAMESPACE, HippoNodeType.HIPPO_NODETYPESRESOURCE, HippoNodeType.HIPPO_NODETYPES, HippoNodeType.HIPPO_CONTENTRESOURCE, HippoNodeType.HIPPO_CONTENT, HippoNodeType.HIPPO_CONTENTROOT, HippoNodeType.HIPPO_CONTENTDELETE, HippoNodeType.HIPPO_CONTENTPROPSET, HippoNodeType.HIPPO_CONTENTPROPADD }) {
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
                            Node initializeNode = rootSession.getRootNode().getNode("hippo:configuration/hippo:initialize");
                            Set<String> tags = new TreeSet<String>();
                            if (initializeNode.hasProperty(HippoNodeType.HIPPO_VERSION)) {
                                for (Value value : initializeNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues()) {
                                    tags.add(value.getString());
                                }
                            }
                            Value[] added = mergeInitializationNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues();
                            for (Value value : added) {
                                tags.add(value.getString());
                            }
                            initializeNode.setProperty(HippoNodeType.HIPPO_VERSION, tags.toArray(new String[tags.size()]));
                        }
                        mergeInitializationNode.remove();
                        rootSession.save();
                    } catch (PathNotFoundException ex) {
                        log.error("Rejected old style configuration content", ex);
                        for(NodeIterator removeTempIter = rootSession.getRootNode().getNode("hippo:configuration/hippo:temporary").getNodes(); removeTempIter.hasNext(); ) {
                            removeTempIter.nextNode().remove();
                        }
                        rootSession.getRootNode().getNode("hippo:configuration/hippo:temporary").save();
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

            LoadInitializationModule.refresh(rootSession);

            if (!hasHippoNamespace) {
                Session initializeSession = DecoratorFactoryImpl.getSessionDecorator(jcrRootSession.impersonate(new SimpleCredentials("system", new char[] {})));
                UpdaterEngine.migrate(initializeSession);
                initializeSession.logout();
            }

            for(DaemonModule module : new Modules<DaemonModule>(Modules.getModules(), DaemonModule.class)) {
                Session moduleSession = syncSession.impersonate(new SimpleCredentials("system", new char[]{}));
                moduleSession = DecoratorFactoryImpl.getSessionDecorator(moduleSession);
                try {
                    module.initialize(moduleSession);
                    daemonModules.add(module);
                } catch(RepositoryException ex) {
                    log.error("Module "+module.toString()+" failed to initialize", ex);
                }
            }

            syncSession.logout(); // the spawned impersonated sessions should remain active though

        } catch (LoginException ex) {
            log.error("no access to repository by repository itself", ex);
        }
    }

    static void initializeNamespace(NamespaceRegistry nsreg, String prefix, String uri)
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

    static void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws ParseException,
            RepositoryException {
        CompactNodeTypeDefReader<QNodeTypeDefinition,NamespaceMapping> cndReader = new HippoCompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(new InputStreamReader(cndStream), cndName, workspace.getNamespaceRegistry(), new QDefinitionBuilderFactory());
        List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (Iterator<QNodeTypeDefinition> iter = ntdList.iterator(); iter.hasNext();) {
            QNodeTypeDefinition ntd = iter.next();

            try {
                ntreg.unregisterNodeType(ntd.getName());
            } catch (NoSuchNodeTypeException ex) {
                // new type, ignore
            } catch (RepositoryException ex) {
                // kind of safe to ignore
            }

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
                            e.printStackTrace();
                        }
                    }
                } else {
                    log.error(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), ex);
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    log.warn(ex.getMessage()+". In " + cndName +" error for "+  ntd.getName().getNamespaceURI() +":"+ntd.getName().getLocalName(), ex);
                    ex.printStackTrace();
                }
            }
        }
    }

    static void removeNodecontent(Session session, String absPath, boolean save) {
        if ("".equals(absPath) || "/".equals(absPath)) {
            log.warn("Not allowed to delete rootNode from initialization.");
            return;
        }

        String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
        try {
            if (relpath.length() > 0 && session.getRootNode().hasNode(relpath)) {
                session.getRootNode().getNode(relpath).remove();
                if (save) {
                    session.save();
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

    static void initializeNodecontent(Session session, String absPath, InputStream istream, String location) {
        try {
            String relpath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            ((HippoSession) session).importDereferencedXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
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
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
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
                log.error("Error initializing content for "+location+" in '" + absPath + "' : " + ex.getClass().getName() + ": " + ex.getMessage());
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

    @Override
    public synchronized void close() {
        for(DaemonModule module : daemonModules) {
            module.shutdown();
        }
        daemonModules.clear();

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

        // stop all listeners on rootSession
        if (rootSession != null && rootSession.isLive()) {
            try {
                ObservationManager obMgr = rootSession.getWorkspace().getObservationManager();
                EventListenerIterator elIter = obMgr.getRegisteredEventListeners();
                while (elIter.hasNext()) {
                    EventListener el = elIter.nextEventListener();
                    log.debug("Removing EventListener from root session");
                    obMgr.removeEventListener(el);
                }
            } catch (Exception ex) {
                log.error("Error while removing listener: " + ex.getMessage(), ex);
            }
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
