/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: BaseRepositoryTest.java 172679 2013-08-02 14:21:12Z mmilicevic $"
 */
public class BaseRepositoryTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(BaseRepositoryTest.class);
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
