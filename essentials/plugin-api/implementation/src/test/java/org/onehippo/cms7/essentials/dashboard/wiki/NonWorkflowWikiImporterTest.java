package org.onehippo.cms7.essentials.dashboard.wiki;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class NonWorkflowWikiImporterTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(NonWorkflowWikiImporterTest.class);

    @Test
    public void testImportAction() throws Exception {
        NonWorkflowWikiImporter importer = new NonWorkflowWikiImporter();
        final Node content = session.getRootNode().addNode("content");
        session.save();
        importer.importAction(session, 10, 0, 5, 2, 3, null, "/content", false, "mytestproject:newsdocument");
        assertNotNull(content.getNodes().nextNode().getNodes().nextNode());

    }
}
