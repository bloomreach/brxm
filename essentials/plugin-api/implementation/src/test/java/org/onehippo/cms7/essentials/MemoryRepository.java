/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: MemoryRepository.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public class MemoryRepository {

    public static final String[] CND_FILE_NAMES = {"/test_cnd.cnd", "/test_hippo.cnd", "/test_hippostd.cnd", "/test_hippostd.cnd", "/test_hippo_sys_edit.cnd", "/test_hippotranslation.cnd", "/test_hippo_gal.cnd", "/mytestproject.cnd" };
    private static Logger log = LoggerFactory.getLogger(MemoryRepository.class);
    private static String configFileName = "repository.xml";
    private static URL resource = MemoryRepository.class.getClassLoader().getResource(configFileName);
    private Session session;
    private File storageDirectory;
    private TransientRepository memoryRepository;

    public MemoryRepository() throws Exception {
        initialize();
        NodeTypeManagerImpl mgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
        for (String fileName : CND_FILE_NAMES) {
            log.info("Registering CND file *{}*", fileName);
            InputStream stream = getClass().getResourceAsStream(fileName);
            mgr.registerNodeTypes(stream, "text/x-jcr-cnd");
        }
    }

    public MemoryRepository(String[] cnds) throws Exception {
        initialize();
        NodeTypeManagerImpl mgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
        for (String fileName : cnds) {
            fileName = String.format("%s%s", '/', fileName);
            log.info("Registering CND file *{}*", fileName);
            InputStream stream = getClass().getResourceAsStream(fileName);
            mgr.registerNodeTypes(stream, "text/x-jcr-cnd");
        }
    }

    private void initialize() throws Exception {
        storageDirectory = new File(System.getProperty("java.io.tmpdir"), "jcr");
        deleteDirectory(storageDirectory);
        memoryRepository = new TransientRepository(RepositoryConfig.create(resource.toURI(), storageDirectory.getAbsolutePath()));
        // initialize session
        session = getSession();
    }

    public File getStorageDirectory() {
        return storageDirectory;
    }

    public void shutDown() {
        if (this.memoryRepository != null) {
            storageDirectory = null;
            if (session != null) {
                session.logout();
            }
            // gc it
            memoryRepository = null;
        }
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.error("Failed to delete: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        return path.delete();
    }

    public Session getSession() throws RepositoryException {
        if (session != null) {
            return session;
        }
        session = memoryRepository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        return session;
    }
}
