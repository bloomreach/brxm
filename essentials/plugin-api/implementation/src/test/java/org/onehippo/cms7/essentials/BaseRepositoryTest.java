/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @version "$Id: BaseRepositoryTest.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public abstract class BaseRepositoryTest extends BaseTest {



    protected MemoryRepository repository;
    protected Session session;
    protected Session hippoSession;

    @Override
    public PluginContext getContext() {
        final TestPluginContext testPluginContext = new TestPluginContext(repository, null);
        testPluginContext.setBeansPackageName("org.onehippo.essentials.test.beans");
        testPluginContext.setRestPackageName("org.onehippo.essentials.test.rest");
        return testPluginContext;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        repository = new MemoryRepository();
        session = repository.getSession();

    }

    @Override
    @After
    public void tearDown() throws Exception {

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

}
