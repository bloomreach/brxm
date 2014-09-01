/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class BaseRepositoryTest extends BaseTest {


    private static final Logger log = LoggerFactory.getLogger(BaseRepositoryTest.class);
    protected MemoryRepository repository;
    protected Session hippoSession;

    @Override
    public PluginContext getContext() {
        final TestPluginContext testPluginContext = new TestPluginContext(repository, null);
        testPluginContext.setComponentsPackageName("org.onehippo.essentials.test.components");
        testPluginContext.setBeansPackageName("org.onehippo.essentials.test.beans");
        testPluginContext.setRestPackageName("org.onehippo.essentials.test.rest");
        return testPluginContext;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        log.info("======================================");
        log.info("setUp()");
        log.info("======================================");
        projectSetup();
        repository = new MemoryRepository();
    }

    public void projectSetup() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        log.info("======================================");
        log.info("tearDown()");
        log.info("======================================");
        super.tearDown();
        if (repository != null) {
            repository.shutDown();
        }
        if (hippoSession != null) {
            hippoSession.logout();
        }
    }

    //############################################
    // UTILITY METHODS
    //############################################
    public void createHstRootConfig() throws RepositoryException {
        final Session session = repository.getSession();
        final Node rootNode = session.getRootNode();
        final Node siteRootNode = rootNode.addNode("hst:hst", "hst:hst");
        final Node configs = siteRootNode.addNode("hst:configurations", "hst:configurations");
        final Node siteNode = configs.addNode(getContext().getProjectNamespacePrefix(), "hst:configuration");
        siteNode.addNode("hst:sitemap", "hst:sitemap");
        siteNode.addNode("hst:pages", "hst:pages");
        siteNode.addNode("hst:components", "hst:components");
        siteNode.addNode("hst:catalog", "hst:catalog");
        siteNode.addNode("hst:sitemenus", "hst:sitemenus");
        siteNode.addNode("hst:templates", "hst:templates");
        session.save();
        session.logout();
    }

    /**
     * Method useful when testing with local build (useful when you wanna see the changes)
     *
     * @return remote session (HippoSession)
     */
    public Session getHippoSession() throws RepositoryException {
        if (hippoSession == null) {

            final HippoRepository hippoRepository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            hippoSession = hippoRepository.login("admin", "admin".toCharArray());
        }
        return hippoSession;

    }

    public Session getSession(){
        try {
            return repository.getSession();
        } catch (RepositoryException e) {
           // ignore
        }
        return null;
    }

}
