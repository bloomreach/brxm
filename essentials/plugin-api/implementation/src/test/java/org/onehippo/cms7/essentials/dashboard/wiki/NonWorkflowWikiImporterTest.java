package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Properties;

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
public class NonWorkflowWikiImporterTest{// extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(NonWorkflowWikiImporterTest.class);

/*    @Test
    public void testImportAction() throws Exception {

        final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        Session rmiSession = repository.login("admin", "admin".toCharArray());
        NonWorkflowWikiImporter importer = new NonWorkflowWikiImporter();
        final Node content = rmiSession.getRootNode().addNode("content/documents/test9");
        final Node images = rmiSession.getRootNode().addNode("content/gallery/images9", "hippogallery:stdImageGallery");

        Properties properties = new Properties();

        properties.put("amount", 99);
        properties.put("offset", 0);
        properties.put("maxSubFolder", 5);
        properties.put("maxDocsPerFolder", 3);
//        properties.put("filesystemLocation", null);
        properties.put("siteContentBasePath", "/content/documents/test6");
        properties.put("imageContentBasePath", "/content/gallery/test6");
        properties.put("addImages", true);
        properties.put("strategy", new NewsWikiStrategy(properties));

        rmiSession.save();
        importer.importAction(rmiSession, 99, 0, 2, 5, 3, null, "/content/documents/test9", false, new NewsWikiStrategy());
//        assertNotNull(content.getNodes().nextNode().getNodes().nextNode());
        rmiSession.logout();

    }*/
}
