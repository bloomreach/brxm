/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: MemoryRepository.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public class MemoryRepository {

    public static final String[] CND_FILE_NAMES = {"/test_cnd.cnd", "/test_hippo.cnd", "/test_hippo_sys_edit.cnd", "/test_hippo_gal.cnd", "/test_hippotranslation.cnd", "/test_hippostdpubwf.cnd", "/mytestproject.cnd"};
    private static Logger log = LoggerFactory.getLogger(MemoryRepository.class);
    private static String configFileName = "repository.xml";
    private static URL resource = MemoryRepository.class.getClassLoader().getResource(configFileName);
    private Session session;
    private File storageDirectory;
    private TransientRepository memoryRepository;

    /** namespace prefix constant */
   // public static final String OCM_NAMESPACE_PREFIX = "ocm";
    /**
     * namespace constant
     */
   // public static final String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";


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
        //ocm mapping
       // registerNamespace(session);
        //registerNodeTypes(session);
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

//    OCM MAPPING
//    protected void registerNamespace(final Session session) throws javax.jcr.RepositoryException {
//        log.info("Register namespace");
//        String[] jcrNamespaces = session.getWorkspace().getNamespaceRegistry().getPrefixes();
//        boolean createNamespace = true;
//        for (int i = 0; i < jcrNamespaces.length; i++) {
//            if (jcrNamespaces[i].equals(OCM_NAMESPACE_PREFIX)) {
//                createNamespace = false;
//                log.debug("Jackrabbit OCM namespace exists.");
//            }
//        }
//        if (createNamespace) {
//            session.getWorkspace().getNamespaceRegistry().registerNamespace(OCM_NAMESPACE_PREFIX, OCM_NAMESPACE);
//            log.info("Successfully created Jackrabbit OCM namespace.");
//        }
//
//        if (session.getRootNode() != null) {
//            log.info("Jcr session setup successfull.");
//        }
//    }
//
//    protected void registerNodeTypes(Session session)
//            throws InvalidNodeTypeDefException, javax.jcr.RepositoryException, IOException {
//        InputStream xml = getClass().getResourceAsStream("/custom_nodetypes.xml");
//
//        // HINT: throws InvalidNodeTypeDefException, IOException
//        QNodeTypeDefinition[] types = NodeTypeReader.read(xml);
//
//        Workspace workspace = session.getWorkspace();
//        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
//        NodeTypeRegistry ntReg = ((NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();
//
//        for (int j = 0; j < types.length; j++) {
//            QNodeTypeDefinition def = types[j];
//
//            try {
//                ntReg.getNodeTypeDef(def.getName());
//            } catch (NoSuchNodeTypeException nsne) {
//                // HINT: if not already registered than register custom node type
//                ntReg.registerNodeType(def);
//            }
//
//        }
//    }
}
