/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.impl.InitializationProcessorImpl;
import org.hippoecm.repository.impl.ReferenceWorkspaceImpl;
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.repository.modules.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEFOLDER;

public class LocalHippoRepository extends HippoRepositoryImpl {


    /** System property for overriding the repository path */
    public static final String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for specifying the upgrade flag */
    public static final String SYSTEM_UPGRADE_PROPERTY = "repo.upgrade";

    /** System property for enabling bootstrap */
    public static final String SYSTEM_BOOTSTRAP_PROPERTY = "repo.bootstrap";

    /** System property for overriding the servlet config file */
    public static final String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    /** The advised threshold on the number of modified nodes to hold in transient session state */
    public static int batchThreshold = 96;

    protected static final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    private LocalRepositoryImpl jackrabbitRepository = null;

    /** Whether to generate a dump.xml file of the /hippo:configuration node at shutdown */
    private final boolean dump = false;

    /** Whether to reindex after upgrading */
    private boolean upgradeReindexFlag = false;

    /** Whether to run a derived properties validation after upgrading */
    private boolean upgradeValidateFlag = true;

    private String repoPath;

    /** When during startup a situation is detected that a restart is required, this flag signals this, but only one restart should be appropriate */
    boolean needsRestart = false;

    private static enum UpgradeFlag {
        TRUE, FALSE, ABORT
    }

    private ModuleManager moduleManager;

    protected LocalHippoRepository() throws RepositoryException {
        super();
    }

    protected LocalHippoRepository(String location) throws RepositoryException {
        super(location);
    }

    public static HippoRepository create(String location) throws RepositoryException {
        LocalHippoRepository localHippoRepository;
        if (location == null) {
            localHippoRepository = new LocalHippoRepository();
        } else {
            localHippoRepository = new LocalHippoRepository(location);
        }
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
        if (repoPath != null) {
            return repoPath;
        }

        final String pathProp = System.getProperty(SYSTEM_PATH_PROPERTY);

        if (pathProp == null || pathProp.isEmpty()) {
            repoPath = getWorkingDirectory();
        } else if (pathProp.charAt(0) == '.') {
            // relative path
            repoPath = getWorkingDirectory() + System.getProperty("file.separator") + pathProp;
        } else {
            repoPath = RepoUtils.stripFileProtocol(pathProp);
        }

        if (repoPath.startsWith("~" + File.separator)) {
            repoPath = System.getProperty("user.home") + repoPath.substring(1);
        }

        log.info("Using repository path: " + repoPath);
        return repoPath;
    }

    /**
     * If the "file://" protocol is used, the path MUST be absolute.
     * In all other cases the config file is used as a class resource.
     * @return InputStream to the repository config
     * @throws RepositoryException
     */
    private static InputStream getRepositoryConfigAsStream() throws RepositoryException {
        String configPath = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        if (configPath == null || "".equals(configPath)) {
            configPath = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        if (configPath == null || "".equals(configPath)) {
            log.info("Using default repository config: " + DEFAULT_REPOSITORY_CONFIG);
            return LocalHippoRepository.class.getResourceAsStream(DEFAULT_REPOSITORY_CONFIG);
        }

        if (!configPath.startsWith("file:")) {
            log.info("Using resource repository config: " + configPath);
            return LocalHippoRepository.class.getResourceAsStream(configPath);
        }

        configPath = RepoUtils.stripFileProtocol(configPath);

        log.info("Using file repository config: file:/" + configPath);

        File configFile = new File(configPath);
        try {
            return new BufferedInputStream(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            throw new RepositoryException("Repository config not found: file:/" + configPath);
        }
    }

    private class LocalRepositoryImpl extends RepositoryImpl {
        LocalRepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
            super(repConfig);
        }
        @Override
        public Session getRootSession(String workspaceName) throws RepositoryException {
            return super.getRootSession(workspaceName);
        }
        void enableVirtualLayer(boolean enabled) throws RepositoryException {
            isStarted = enabled;
        }

        protected FileSystem getFileSystem() {
            return super.getFileSystem();
        }

        private boolean isClustered() {
            return getRepositoryConfig().getClusterConfig() != null;
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
            ((HippoSecurityManager) jackrabbitRepository.getSecurityManager()).configure();
            if (upgradeValidateFlag) {
                log.warn("post migration cycle validating content");
                final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
                SessionDecorator session = DecoratorFactoryImpl.getSessionDecorator(
                        jackrabbitRepository.getRootSession(null).impersonate(
                                credentials), credentials);
                session.postValidation();
                session.logout();
            }
        } else {
            ((HippoSecurityManager) jackrabbitRepository.getSecurityManager()).configure();
        }
    }

    private void initializeReindex() {
        final File basedir = new File(getRepositoryPath());
        try {
            FileUtils.deleteDirectory(new File(basedir, "repository/index"));
            FileUtils.deleteDirectory(new File(basedir, "workspaces/default/index"));
        } catch (IOException e) {
            log.warn("Unable to delete index", e);
        }
    }

    private void initializeStartup() throws RepositoryException {

        Modules.setModules(new Modules(Thread.currentThread().getContextClassLoader()));

        final RepositoryConfig repConfig = RepositoryConfig.create(getRepositoryConfigAsStream(), getRepositoryPath());
        jackrabbitRepository = new LocalRepositoryImpl(repConfig);

        repository = new DecoratorFactoryImpl().getRepositoryDecorator(jackrabbitRepository);

        try {
            // get the current root/system session for the default workspace for namespace and nodetypes init
            Session jcrRootSession =  jackrabbitRepository.getRootSession(null);

            if(!jcrRootSession.getRootNode().isNodeType("mix:referenceable")) {
                jcrRootSession.getRootNode().addMixin("mix:referenceable");
                jcrRootSession.save();
            }

            final boolean initializedBefore = initializedBefore(jcrRootSession);
            if (initializedBefore) {
                switch(readUpgradeFlag()) {
                case TRUE:
                    jackrabbitRepository.enableVirtualLayer(false);
                    migrate(jcrRootSession);
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

            if (!initializedBefore || isContentBootstrapEnabled()) {
                final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
                final SessionDecorator bootstrapSession = DecoratorFactoryImpl.getSessionDecorator(jcrRootSession.impersonate(credentials), credentials);
                initializeSystemNodeTypes(bootstrapSession, jackrabbitRepository.getFileSystem());
                contentBootstrap(bootstrapSession);
                bootstrapSession.logout();
            }

            jackrabbitRepository.enableVirtualLayer(true);

            moduleManager = new ModuleManager(jcrRootSession.impersonate(new SimpleCredentials("system", new char[]{})));
            moduleManager.start();

        } catch (LoginException ex) {
            log.error("no access to repository by repository itself", ex);
        }
    }

    private boolean initializedBefore(final Session systemSession) throws RepositoryException {
        return systemSession.getWorkspace().getNodeTypeManager().hasNodeType(NT_INITIALIZEFOLDER);
    }

    private UpgradeFlag readUpgradeFlag() {
        UpgradeFlag upgradeFlag = UpgradeFlag.TRUE;
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

        return upgradeFlag;
    }

    private boolean isContentBootstrapEnabled() {
        return Boolean.getBoolean(SYSTEM_BOOTSTRAP_PROPERTY);
    }

    private void contentBootstrap(final Session systemSession) throws RepositoryException {
        final InitializationProcessorImpl initializationProcessor = new InitializationProcessorImpl();

        if (!systemSession.getRootNode().hasNode("hippo:configuration")) {
            log.info("Initializing configuration content");
            InputStream configuration = getClass().getResourceAsStream("configuration.xml");
            if (configuration != null) {
                initializationProcessor.initializeNodecontent(systemSession, "/", configuration, getClass().getPackage().getName() + ".configuration.xml");
            } else {
                log.error("Could not initialize configuration content: ResourceAsStream not found: configuration.xml");
            }
            systemSession.save();
        } else {
            log.info("Initial configuration content already present");
        }

        // load all extension resources
        try {
            initializationProcessor.loadExtensions(systemSession);
        } catch (IOException ex) {
            throw new RepositoryException("Could not obtain initial configuration from classpath", ex);
        }
        initializationProcessor.processInitializeItems(systemSession);
        if (log.isDebugEnabled()) {
            initializationProcessor.dryRun(systemSession);
        }
    }

    /**
     * Migration via the UpdaterEngine is enabled when the UpdaterEngine is on the classpath.
     * Users must explicitly add the dependency to their project in order to use the old
     * style updaters.
     */
    private void migrate(final Session jcrRootSession) throws RepositoryException {
        try {
            final Class<?> updaterEngineClass = Class.forName("org.hippoecm.repository.updater.UpdaterEngine");
            final Method migrate = updaterEngineClass.getMethod("migrate", Session.class, boolean.class);
            final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
            final Session migrateSession = DecoratorFactoryImpl.getSessionDecorator(
                    jcrRootSession.impersonate(credentials), credentials);
            needsRestart = (Boolean) migrate.invoke(null, migrateSession, jackrabbitRepository.isClustered());
            migrateSession.logout();
        } catch (ClassNotFoundException ignore) {
            log.debug("UpdaterEngine not found");
        } catch (NoSuchMethodException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        } catch (InvocationTargetException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        } catch (IllegalAccessException e) {
            log.error("Unexpected error while trying to invoke UpdaterEngine", e);
        }
    }

    private void initializeSystemNodeTypes(final Session systemSession, final FileSystem fileSystem) throws RepositoryException {
        final Session syncSession = systemSession.impersonate(new SimpleCredentials("system", new char[] {}));
        final InitializationProcessorImpl initializationProcessor = new InitializationProcessorImpl();

        final Properties checksumProperties = new Properties();
        try {
            if (fileSystem.exists("/cnd-checksums")) {
                InputStream in = null;
                try {
                    in = fileSystem.getInputStream("/cnd-checksums");
                    checksumProperties.load(in);
                } catch (IOException e) {
                    log.error("Failed to read cnd checksum file. All system cnds will be reloaded.", e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        } catch (FileSystemException e) {
            log.error("Failed to read cnd checksum from the file system. All system cnds will be reloaded", e);
        }
        for(String cndName : new String[] { "hippo.cnd", "hipposys.cnd", "hipposysedit.cnd", "hippofacnav.cnd", "hipposched.cnd" }) {
            InputStream cndStream = null;
            try {
                cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                final String checksum = getChecksum(cndStream);
                cndStream.close();
                if (!checksum.equals(checksumProperties.getProperty(cndName))) {
                    log.info("Initializing nodetypes from: " + cndName);
                    cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                    initializationProcessor.initializeNodetypes(syncSession.getWorkspace(), cndStream, cndName);
                    syncSession.save();
                    checksumProperties.setProperty(cndName, checksum);
                } else {
                    log.info("No need to reload " + cndName + ": no changes");
                }
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
            } catch (NoSuchAlgorithmException e) {
                throw new RepositoryException("Could not initialize repository with hippo node types", e);
            } catch (IOException e) {
                throw new RepositoryException("Could not initialize repository with hippo node types", e);
            } finally {
                if (cndStream != null) { try { cndStream.close(); } catch (IOException ignore) {} }
            }

        }

        OutputStream out = null;
        try {
            out = fileSystem.getOutputStream("/cnd-checksums");
            checksumProperties.store(out, null);
        } catch (IOException e) {
            log.error("Failed to store cnd checksum file.", e);
        } catch (FileSystemException e) {
            log.error("Failed to store cnd checksum file.", e);
        } finally {
            IOUtils.closeQuietly(out);
        }

        syncSession.logout();
    }

    private String getChecksum(final InputStream cndStream) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md5 = MessageDigest.getInstance("SHA-256");
        final byte[] buffer = new byte[1024];
        int read;
        do {
            read = cndStream.read(buffer, 0, buffer.length);
            if (read > 0) {
                md5.update(buffer, 0, read);
            }
        } while (read > 0);

        final byte[] bytes = md5.digest();
        //convert the byte to hex format
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @Override
    public synchronized void close() {

        if (moduleManager != null) {
            moduleManager.stop();
            moduleManager = null;
        }

        Session session = null;
        if (dump && repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/hippo:configuration", out, false, false);
            } catch (IOException ex) {
                log.error("Error while dumping configuration: " + ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                log.error("Error while dumping configuration: " + ex.getMessage(), ex);
            } finally {
                if (session != null) {
                    session.logout();
                }
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

    @Override
    public InitializationProcessor getInitializationProcessor() {
        return new InitializationProcessorImpl();
    }

    @Override
    public ReferenceWorkspace getOrCreateReferenceWorkspace() throws RepositoryException {
        return new ReferenceWorkspaceImpl(jackrabbitRepository);
    }

}
