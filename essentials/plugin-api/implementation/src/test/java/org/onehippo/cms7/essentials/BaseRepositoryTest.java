/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

import javax.jcr.Session;

/**
 * @version "$Id: BaseRepositoryTest.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public class BaseRepositoryTest extends BaseTest {

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


}
