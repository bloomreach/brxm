package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.InputStream;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class UpdateUtilsTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(UpdateUtilsTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Node updaterNode = session.getNode(UpdateUtils.UPDATE_UTIL_PATH);
        if (!updaterNode.hasNode(UpdateUtils.UpdateType.REGISTRY.getPath())) {
            updaterNode.addNode(UpdateUtils.UpdateType.REGISTRY.getPath(), "hipposys:updaterfolder");
        }
    }

    @Test
    public void testUpdateStreamUtil() throws Exception {
        PluginContext context = new TestPluginContext(repository, null);
        final InputStream resourceAsStream = getClass().getResourceAsStream("/updateplugintest.xml");
        UpdateUtils.addToRegistry(context, resourceAsStream);
        assertTrue(session.itemExists(UpdateUtils.UPDATE_UTIL_PATH + UpdateUtils.UpdateType.REGISTRY.getPath() + "/new-1"));
    }
}
