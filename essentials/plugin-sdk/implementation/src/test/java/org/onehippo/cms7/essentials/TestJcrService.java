/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJcrService implements JcrService {

    private static final Logger LOG = LoggerFactory.getLogger(TestJcrService.class);

    private HippoRepository hippoRepository;
    private MemoryRepository memoryRepository;

    public void setUp() throws Exception {
        memoryRepository = new MemoryRepository();
    }

    public void tearDown() {
        if (memoryRepository != null) {
            memoryRepository.shutDown();
        }
    }

    public void setHippoRepository(final HippoRepository hippoRepository) {
        this.hippoRepository = hippoRepository;
    }

    public void createHstRootConfig(final String projectNamespacePrefix) throws RepositoryException {
        final Session session = memoryRepository.getSession();
        final Node rootNode = session.getRootNode();
        final Node siteRootNode = rootNode.addNode("hst:hst", "hst:hst");
        final Node configs = siteRootNode.addNode("hst:configurations", "hst:configurations");
        final Node siteNode = configs.addNode(projectNamespacePrefix, "hst:configuration");
        siteNode.addNode("hst:sitemap", "hst:sitemap");
        siteNode.addNode("hst:pages", "hst:pages");
        siteNode.addNode("hst:components", "hst:components");
        siteNode.addNode("hst:catalog", "hst:catalog");
        siteNode.addNode("hst:sitemenus", "hst:sitemenus");
        siteNode.addNode("hst:templates", "hst:templates");
        session.save();
        session.logout();
    }

    public void registerNodeTypes(final String cndResourcePath) throws RepositoryException, IOException {
        memoryRepository.registerNodeTypes(cndResourcePath);
    }

    @Override
    public Session createSession() {
        try {
            if (hippoRepository != null) {
                Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
                return hippoRepository.login(credentials);
            }
            return memoryRepository.getSession();
        } catch (RepositoryException e) {
            LOG.error("Failed to create JCR session.", e);
        }
        return null;
    }

    @Override
    public void refreshSession(final Session session, final boolean keepChanges) {
        try {
            session.refresh(keepChanges);
        } catch (RepositoryException e) {
            LOG.error("Failed to refresh session.", e);
        }
    }

    @Override
    public void destroySession(final Session session) {
        session.logout();
    }
}
