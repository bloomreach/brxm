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
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.onehippo.repository.bootstrap.InitializationProcessor;
import org.onehippo.repository.bootstrap.PostStartupTask;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.onehippo.repository.bootstrap.InitializationProcessorImpl;
import org.hippoecm.repository.impl.ReferenceWorkspaceImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.onehippo.repository.modules.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEFOLDER;
import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;

public class LocalHippoRepository extends HippoRepositoryImpl {


    /** System property for overriding the repository path */
    public static final String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for defining the base path for a non-absolute repo.path property */
    public static final String SYSTEM_BASE_PATH_PROPERTY = "repo.base.path";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_CONFIG_PROPERTY = "repo.config";

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

    private String repoPath;
    private String repoConfig;

    private ModuleManager moduleManager;

    protected LocalHippoRepository() {
        super();
    }

    protected LocalHippoRepository(String repositoryConfig) throws RepositoryException {
        super();
        this.repoConfig = repositoryConfig;
    }

    protected LocalHippoRepository(String repositoryDirectory, String repositoryConfig) throws RepositoryException {
        super(repositoryDirectory);
        this.repoConfig = repositoryConfig;
    }

    public static HippoRepository create(String repositoryDirectory) throws RepositoryException {
        return create(repositoryDirectory, null);
    }

    public static HippoRepository create(String repositoryDirectory, String repositoryConfig) throws RepositoryException {
        LocalHippoRepository localHippoRepository;
        if (repositoryDirectory == null) {
            localHippoRepository = new LocalHippoRepository(repositoryConfig);
        } else {
            localHippoRepository = new LocalHippoRepository(repositoryDirectory, repositoryConfig);
        }
        localHippoRepository.initialize();
        VMHippoRepository.register(repositoryDirectory, localHippoRepository);
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
     * <p>
     * The system property repo.path can be used to override the default.
     * </p>
     * If repo.path has an absolute path, or system property repo.base.path is undefined/empty, the repo.path
     * is assumed to be an absolute path and returned as such
     * </p>
     * <p>
     * If repo.path starts with '~/' the '~' is expanded to the user.home location, thereby becoming an absolute
     * path and returns as repository path.
     * </p>
     * <p>
     * Else, when repo.path is not an absolute path and system property repo.base.path also is defined,
     * the repo.path is taken relative to the repo.base.path.
     *
     * @return The absolute path to the file repository
     */
    protected String getRepositoryPath() {
        if (repoPath != null) {
            return repoPath;
        }

        String path = System.getProperty(SYSTEM_PATH_PROPERTY);
        if (path != null) {
            if (path.isEmpty()) {
                path = null;
            }
            else {
                path = RepoUtils.stripFileProtocol(path);
                if (path.startsWith("~" + File.separator)) {
                    path = System.getProperty("user.home") + path.substring(1);
                }
            }
        }

        String basePath = path != null ? System.getProperty(SYSTEM_BASE_PATH_PROPERTY) : null;

        if (basePath != null ) {
            if (basePath.isEmpty()) {
                basePath = null;
            }
            else {
                basePath = RepoUtils.stripFileProtocol(basePath);
            }
        }

        if (path == null) {
            repoPath = getWorkingDirectory();
        }
        else if (new File(path).isAbsolute() || basePath == null) {
                repoPath = path;
        }
        else {
            repoPath = basePath + System.getProperty("file.separator") + path;
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
    private InputStream getRepositoryConfigAsStream() throws RepositoryException {

        String configPath = repoConfig;

        if (StringUtils.isEmpty(configPath)) {
            configPath = System.getProperty(SYSTEM_CONFIG_PROPERTY);
        }

        if (StringUtils.isEmpty(configPath)) {
            configPath = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        if (StringUtils.isEmpty(configPath)) {
            configPath = DEFAULT_REPOSITORY_CONFIG;
        }

        if (!configPath.startsWith("file:")) {
            final URL configResource = LocalHippoRepository.class.getResource(configPath);
            log.info("Using resource repository config: " + configResource);
            try {
                return configResource.openStream();
            } catch (IOException e) {
                throw new RepositoryException("Failed to open repository configuration", e);
            }
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

    }

    protected void initialize() throws RepositoryException {
        log.info("Initializing Hippo Repository");

        Modules.setModules(new Modules(Thread.currentThread().getContextClassLoader()));

        final RepositoryConfig repConfig = RepositoryConfig.create(getRepositoryConfigAsStream(), getRepositoryPath());
        jackrabbitRepository = new LocalRepositoryImpl(repConfig);

        repository = new DecoratorFactoryImpl().getRepositoryDecorator(jackrabbitRepository);
        Session bootstrapSession = null, lockSession = null;
        final InitializationProcessorImpl initializationProcessor = new InitializationProcessorImpl();
        boolean locked = false;

        try {
            final Session rootSession =  jackrabbitRepository.getRootSession(null);
            ensureRootIsReferenceable(rootSession);

            final boolean initializedBefore = initializedBefore(rootSession);
            List<PostStartupTask> postStartupTasks = Collections.emptyList();

            if (!initializedBefore || isContentBootstrapEnabled()) {
                final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
                bootstrapSession = DecoratorFactoryImpl.getSessionDecorator(rootSession.impersonate(credentials), credentials);
                lockSession = DecoratorFactoryImpl.getSessionDecorator(rootSession.impersonate(credentials), credentials);
                initializeSystemNodeTypes(initializationProcessor, bootstrapSession, jackrabbitRepository.getFileSystem());
                if (!bootstrapSession.nodeExists("/hippo:configuration")) {
                    log.debug("Initializing configuration content");
                    BootstrapUtils.initializeNodecontent(bootstrapSession, "/", getClass().getResource("configuration.xml"));
                    bootstrapSession.save();
                } else {
                    log.debug("Initial configuration content already present");
                }
                initializationProcessor.lock(lockSession);
                locked = true;
                postStartupTasks = contentBootstrap(initializationProcessor, bootstrapSession);
            }

            jackrabbitRepository.enableVirtualLayer(true);

            moduleManager = new ModuleManager(rootSession.impersonate(new SimpleCredentials("system", new char[]{})));
            moduleManager.start();

            log.debug("Executing post-startup tasks");
            for (PostStartupTask task : postStartupTasks) {
                task.execute();
            }

            ((HippoSecurityManager) jackrabbitRepository.getSecurityManager()).configure();
        } finally {
            if (lockSession != null) {
                if (locked) {
                    initializationProcessor.unlock(lockSession);
                }
                lockSession.logout();
            }
            if (bootstrapSession != null) {
                bootstrapSession.logout();
            }
        }
    }

    private void ensureRootIsReferenceable(final Session rootSession) throws RepositoryException {
        if(!rootSession.getRootNode().isNodeType(MIX_REFERENCEABLE)) {
            rootSession.getRootNode().addMixin(MIX_REFERENCEABLE);
            rootSession.save();
        }
    }

    private boolean initializedBefore(final Session systemSession) throws RepositoryException {
        return systemSession.getWorkspace().getNodeTypeManager().hasNodeType(NT_INITIALIZEFOLDER);
    }

    private boolean isContentBootstrapEnabled() {
        return Boolean.getBoolean(SYSTEM_BOOTSTRAP_PROPERTY);
    }

    private List<PostStartupTask> contentBootstrap(final InitializationProcessorImpl initializationProcessor, final Session systemSession) throws RepositoryException {
        final List<Node> pendingItems;
        try {
            pendingItems = initializationProcessor.loadExtensions(systemSession);
        } catch (IOException ex) {
            throw new RepositoryException("Could not obtain initial configuration from classpath", ex);
        }
        return initializationProcessor.processInitializeItems(systemSession, pendingItems);
    }

    private void initializeSystemNodeTypes(final InitializationProcessorImpl initializationProcessor, final Session systemSession, final FileSystem fileSystem) throws RepositoryException {
        final Session syncSession = systemSession.impersonate(new SimpleCredentials("system", new char[] {}));

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
                    BootstrapUtils.initializeNodetypes(syncSession, cndStream, cndName);
                    syncSession.save();
                    checksumProperties.setProperty(cndName, checksum);
                } else {
                    log.info("No need to reload " + cndName + ": no changes");
                }
            } catch (NoSuchAlgorithmException | RepositoryException | IOException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } finally {
                if (cndStream != null) { try { cndStream.close(); } catch (IOException ignore) {} }
            }

        }

        OutputStream out = null;
        try {
            out = fileSystem.getOutputStream("/cnd-checksums");
            checksumProperties.store(out, null);
        } catch (IOException|FileSystemException e) {
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
        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                log.error("Error while shutting down Jackrabbit", ex);
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
