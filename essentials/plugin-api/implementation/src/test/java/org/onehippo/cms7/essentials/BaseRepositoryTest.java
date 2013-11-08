/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: BaseRepositoryTest.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public class BaseRepositoryTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseRepositoryTest.class);
    protected MemoryRepository repository;
    protected Session session;

    @Override
    public PluginContext getContext() {
        final TestPluginContext context = (TestPluginContext) super.getContext();
        context.setSession(session);


        return context;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        repository = new MemoryRepository();
        session = repository.getSession();

    }

    @After
    public void tearDown() throws Exception {

        super.tearDown();
        if (repository != null) {
            repository.shutDown();
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


}
