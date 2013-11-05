package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.InputStream;

import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @version "$Id$"
 */
@Ignore("please use MemoryRepository and no RMI connections")
public class UpdateUtilsTest {

    private static Logger log = LoggerFactory.getLogger(UpdateUtilsTest.class);

    private Session session;

    @Before
    public void setUp() throws Exception {
        try {
            final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            session = repository.login("admin", "admin".toCharArray());
        } catch (Exception e) {
            log.error("Error creating repository connection");
            assumeTrue(false);
        }


    }

    @Test
    public void testUpdateStreamUtil() throws Exception {
        PluginContext context = new DashboardPluginContext(session, null);
        final InputStream resourceAsStream = getClass().getResourceAsStream("/updateplugintest.xml");
        UpdateUtils.addToRegistry(context, resourceAsStream);
        assertTrue(session.itemExists(UpdateUtils.UPDATE_UTIL_PATH + UpdateUtils.UpdateType.REGISTRY.getPath() + "/new-1"));
    }

    @After
    public void tearDown() throws Exception {
        session.logout();
    }
}
