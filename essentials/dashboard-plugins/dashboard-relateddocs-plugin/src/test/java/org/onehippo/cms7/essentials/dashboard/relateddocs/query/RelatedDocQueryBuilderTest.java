package org.onehippo.cms7.essentials.dashboard.relateddocs.query;

import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @version "$Id$"
 */
public class RelatedDocQueryBuilderTest {

    private static Logger log = LoggerFactory.getLogger(RelatedDocQueryBuilderTest.class);

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
        if (session.itemExists("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater")) {
            session.getNode("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater").remove();
            session.save();
        }

    }

    @Test
    public void testRelatedDocQueryBuilderTest() throws Exception {
        assertFalse(session.itemExists("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater"));
        PluginContext context = new DashboardPluginContext(session, null, null);
        RelatedDocQueryBuilder builder = new RelatedDocQueryBuilder.Builder().addDocumentType("test:test").build();
        builder.addToRegistry(context);
        assertTrue(session.itemExists("/hippo:configuration/hippo:update/hippo:registry/related-doc-updater"));

    }

    @After
    public void tearDown() throws Exception {
        if(session!=null) {
            session.logout();
        }
    }
}
