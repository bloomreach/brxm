package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Properties;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class NonWorkflowWikiImporterTest extends BaseRepositoryTest {

    @Test
    public void testNonWorkflowJCRImportAction() throws Exception {

        NonWorkflowWikiImporter importer = new NonWorkflowWikiImporter();
        Properties properties = new Properties();

        final Node root = session.getRootNode();
        // already created in MemoryRepository
        assertTrue(root.hasNode("content"));
        final Node content = root.getNode("content");

        final Node documents = content.getNode("documents");
        final Node gallery = content.addNode("gallery", "hippogallery:stdImageGallery");

        documents.addNode("test", "hippostd:folder");
        gallery.addNode("test", "hippogallery:stdImageGallery");

        session.save();

        properties.put("amount", 99);
        properties.put("offset", 0);
        properties.put("maxSubFolder", 5);
        properties.put("maxDocsPerFolder", 3);
        properties.put("prefix", "wiki-");
        properties.put("container", "wikipedia");
        properties.put("simulation", "true");
//        properties.put("filesystemLocation", null);
        properties.put("siteContentBasePath", "/content/documents/test");
        properties.put("imageContentBasePath", "/content/gallery/test");

        importer.importAction(session, new NewsWikiStrategy(properties), properties);

        assertTrue(session.getNode("/content/documents/test").getNodes().getSize() == 1);

        assertTrue(session.nodeExists("/content/documents/test/wikipedia"));

       // session.getNode("/content/documents/test/wikipedia").getNodes();

    }
}
