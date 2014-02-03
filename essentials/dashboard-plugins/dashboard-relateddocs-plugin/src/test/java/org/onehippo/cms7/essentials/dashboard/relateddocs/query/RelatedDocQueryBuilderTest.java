package org.onehippo.cms7.essentials.dashboard.relateddocs.query;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class RelatedDocQueryBuilderTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(RelatedDocQueryBuilderTest.class);

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
    public void testRelatedDocQueryBuilderTest() throws Exception {
        assertFalse(session.itemExists("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater"));
        PluginContext context = new DashboardPluginContext(session, null);
        RelatedDocQueryBuilder builder = new RelatedDocQueryBuilder.Builder().addDocumentType("test:test").build();
        builder.addToRegistry(context);
        assertTrue(session.itemExists("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater"));

    }

}
