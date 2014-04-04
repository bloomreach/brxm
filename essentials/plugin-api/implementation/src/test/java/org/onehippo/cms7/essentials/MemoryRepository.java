/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class MemoryRepository {

    public static final String[] CND_FILE_NAMES = {"/test_cnd.cnd", "/test_hippo.cnd",
            "/test_hippostd.cnd", "/test_hst.cnd",
            "/test_hippo_sys_edit.cnd", "/test_hippotranslation.cnd",
            "/test_hipposys.cnd", "/test_frontend.cnd",
            "/test_editor.cnd", "/test_hippogallerypicker.cnd",
            "/test_hippo_gal.cnd", "/mytestproject.cnd", "/testnamespace.cnd"};
    private static Logger log = LoggerFactory.getLogger(MemoryRepository.class);
    private static String configFileName = "repository.xml";
    private static URL resource = MemoryRepository.class.getClassLoader().getResource(configFileName);

    private final Session session;
    private File storageDirectory;
    private TransientRepository memoryRepository;

    public MemoryRepository() throws Exception {
        initialize();

        session = getSession();
        NodeTypeManagerImpl mgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
        for (String fileName : CND_FILE_NAMES) {
            log.debug("Registering CND file *{}*", fileName);
            InputStream stream = getClass().getResourceAsStream(fileName);
            mgr.registerNodeTypes(stream, "text/x-jcr-cnd", true);
        }

        //add namespace:
        final Node rootNode = session.getRootNode();
        final Node namespaceNode = rootNode.addNode("hippo:namespaces", "hipposysedit:namespacefolder");
        namespaceNode.addNode("testnamespace", "hipposysedit:namespace");
        // add  hippoconfig
        final Node config = rootNode.addNode("hippo:configuration", "hipposys:configuration");
        config.addNode("hippo:workflows", "hipposys:workflowfolder");
        config.addNode("hippo:documents", "hipposys:ocmqueryfolder");
        if (!config.hasNode("hippo:update")) {
            config.addNode("hippo:update", "hipposys:update");
        }

        Node queryNode;
        if (!config.hasNode("hippo:queries")) {
            queryNode = config.addNode("hippo:queries", "hipposys:queryfolder");
        } else {
            queryNode = config.getNode("hippo:queries");
        }
        if (!queryNode.hasNode("hippo:templates")) {
            queryNode.addNode("hippo:templates", "hipposys:queryfolder");
        }
        // add content:
        final Node content = rootNode.addNode("content", "hippostd:folder");

        content.addNode("documents", "hippostd:folder")
                .addNode("testnamespace", "hippostd:folder");
        session.save();
        // mm: todo check out why this fails:
        //session.logout();
    }


    private void initialize() throws Exception {
        storageDirectory = new File(System.getProperty("java.io.tmpdir"), "jcr");
        deleteDirectory(storageDirectory);
        memoryRepository = new TransientRepository(RepositoryConfig.create(resource.toURI(), storageDirectory.getAbsolutePath()));
    }

    public File getStorageDirectory() {
        return storageDirectory;
    }

    public void shutDown() {

        if (this.memoryRepository != null) {
            session.logout();
            memoryRepository.shutdown();
            deleteDirectory(storageDirectory);
            // gc
            storageDirectory = null;
            memoryRepository = null;
        }
    }

    private boolean deleteDirectory(File path) {
        if(path==null){
            return true;
        }
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


    public final Session getSession() throws RepositoryException {
        return memoryRepository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }
}
