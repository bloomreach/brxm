/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MemoryRepository is a wrapper around JackRabbit's TransientRepository. The repository gets instantiated lazily,
 * and is kept alive as long as possible (because registering the test fixture of node types makes unit tests slow).
 * In combination with TestJcrService, only the node test-fixture is reset per test case, while the repository and its
 * registered node types are preserved.
 * If you wish to register additional node types, make sure you reset the repository when you're done, in order not to
 * interfere with other test cases.
 */
class MemoryRepository {

    private static final List<String> CND_FILE_NAMES = Arrays.asList(
            "/test_cnd.cnd",
            "/test_hippo.cnd",
            "/test_hippostd.cnd",
            "/test_hst.cnd",
            "/test_hippo_sys_edit.cnd",
            "/test_hippotranslation.cnd",
            "/test_hipposys.cnd",
            "/test_frontend.cnd",
            "/test_editor.cnd",
            "/test_hippogallerypicker.cnd",
            "/test_hippo_gal.cnd",
            "/test_hippofacnav.cnd",
            "/test_selection_types.cnd",
            "/mytestproject.cnd",
            "/testnamespace.cnd");
    private static final Logger log = LoggerFactory.getLogger(MemoryRepository.class);
    private static final URL REPOSITORY_XML_URL = MemoryRepository.class.getResource("/repository.xml");
    private static final File STORAGE_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), "jcr");;

    private static TransientRepository repository;
    private static Session perpetualSession; // keeps TransientRepository alive

    static {
        clearStorageDirectory();
    }

    /**
     * Call once per test case (see TestJcrService). The first time, the repository gets set up, the namespaces are
     * registered and the node test-fixture is created. Subsequent calls only restore the node test-fixture.
     */
    static void initialize() {
        if (repository == null) {
            try {
                final URI repositoryXmlURI = REPOSITORY_XML_URL.toURI();
                repository = new TransientRepository(RepositoryConfig.create(repositoryXmlURI, STORAGE_DIRECTORY.getAbsolutePath()));
                perpetualSession = createSession();
                CND_FILE_NAMES.forEach(MemoryRepository::registerNodeTypes);
            } catch (Exception e) {
                log.error("Failed te create repository.", e);
            }
        }
        setupTestNodes();
    }

    static Session createSession() {
        try {
            return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        } catch (RepositoryException e) {
            log.error("Failed creating a session.", e);
        }
        return null;
    }

    /**
     * Register additional node types on top of the build-in ones (CND_FILE_NAMES). To keep subsequent usages of
     * the repository independent, use #reset (sparingly) if your test case registers additional node types.
     */
    static void registerNodeTypes(String resourcePath) {
        try {
            NodeTypeManagerImpl mgr = (NodeTypeManagerImpl) perpetualSession.getWorkspace().getNodeTypeManager();
            InputStream stream = MemoryRepository.class.getResourceAsStream(resourcePath);
            mgr.registerNodeTypes(stream, "text/x-jcr-cnd", true);
        } catch (RepositoryException | IOException e) {
            log.error("Failed registering node types for resource '{}'.", resourcePath, e);
        }
    }

    /**
     * Shutdown the repository such that a new one will be created when it is used again.
     *
     * This method should be avoided (because creating a new one is slow), but might need to be used when your
     * test case meddled with the repository's namespaces (created new ones), which must not be there for a subsequent
     * test case. Typically, use this method in conjunction with #registerNodeTypes.
     */
    static void reset() {
        perpetualSession.logout();
        perpetualSession = null;

        repository.shutdown();
        repository = null;

        clearStorageDirectory();
    }

    private static void setupTestNodes() {
        try {
            final Node rootNode = perpetualSession.getRootNode();

            // Wipe out any previous state
            for (Node child : new NodeIterable(rootNode.getNodes())) {
                if (!"jcr:system".equals(child.getName())) {
                    child.remove();
                }
            }

            // /hippo:namespaces
            //   /testnamespace
            final Node namespaceNode = rootNode.addNode("hippo:namespaces", "hipposysedit:namespacefolder");
            namespaceNode.addNode("testnamespace", "hipposysedit:namespace");

            // /hippo:configuration
            //   /hippo:derivatives (auto-created)
            //   /hippo:documents
            //   /hippo:queries (auto-created)
            //     /hippo:templates
            //   /hippo:temporary (auto-created)
            //   /hippo:translations (auto-created)
            //   /hippo:update (auto-created)
            //   /hippo:workflows
            final Node config = rootNode.addNode("hippo:configuration", "hipposys:configuration");
            config.addNode("hippo:documents", "hipposys:ocmqueryfolder");
            config.getNode("hippo:queries").addNode("hippo:templates", "hipposys:queryfolder");
            config.addNode("hippo:workflows", "hipposys:workflowfolder");

            // /hst:hst
            //   /hst:configurations
            //     /testnamespace
            //       /hst:catalog
            //       /hst:components
            //       /hst:pages
            //       /hst:sitemap
            //       /hst:sitemenus
            //       /hst:templates
            final Node siteNode = rootNode.addNode("hst:hst", "hst:hst")
                    .addNode("hst:configurations", "hst:configurations")
                    .addNode(TestSettingsService.PROJECT_NAMESPACE_TEST, "hst:configuration");
            siteNode.addNode("hst:catalog", "hst:catalog");
            siteNode.addNode("hst:components", "hst:components");
            siteNode.addNode("hst:pages", "hst:pages");
            siteNode.addNode("hst:sitemap", "hst:sitemap");
            siteNode.addNode("hst:sitemenus", "hst:sitemenus");
            siteNode.addNode("hst:templates", "hst:templates");

            // /content
            //   /documents
            //     /testnamespace
            rootNode.addNode("content", "hippostd:folder")
                    .addNode("documents", "hippostd:folder")
                    .addNode("testnamespace", "hippostd:folder");

            perpetualSession.save();
        } catch (RepositoryException e) {
            log.error("Failed setting up test nodes.", e);
        }
    }

    private static void clearStorageDirectory() {
        if (STORAGE_DIRECTORY.exists()) {
            try {
                FileUtils.deleteDirectory(STORAGE_DIRECTORY);
            } catch (IOException e) {
                log.error("Failed to delete '{}'.", STORAGE_DIRECTORY.getAbsolutePath(), e);
            }
        }
    }
}
