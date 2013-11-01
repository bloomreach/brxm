package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Arrays;
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestHippoDocument;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestHippoFolder;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestHippoHandle;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestHippoNode;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestHippoPublishableDocument;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestNewsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class TestJcrOM extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(TestJcrOM.class);

    @Test
    public void testNodeCreationWithJcrOM() throws Exception {
        final Session session1 = getContext().getSession();

        final Node rootNode = session1.getRootNode();
        Jcrom jcrom = new Jcrom(new HashSet(Arrays.asList(TestHippoDocument.class, TestHippoFolder.class, TestHippoHandle.class, TestHippoNode.class, TestHippoPublishableDocument.class, TestNewsDocument.class)));

        final TestHippoFolder testHippoFolder = new TestHippoFolder();
        testHippoFolder.setName("testfolder");

        final Node node = jcrom.addNode(rootNode, testHippoFolder);
        assertTrue(node.isNodeType("hippostd:folder") && node.getName().equals("testfolder"));

        final TestHippoFolder testHippoFolder1 = jcrom.fromNode(TestHippoFolder.class, node);
        assertTrue(testHippoFolder1.getPath().equals("/testfolder"));

    }

    @Test
    public void testNodeCreationAndMoreWIthJcrOM() throws Exception {

        final Session session1 = getContext().getSession();

        final Node rootNode = session1.getRootNode();
        Jcrom jcrom = new Jcrom(new HashSet(Arrays.asList(TestHippoDocument.class, TestHippoFolder.class, TestHippoHandle.class, TestHippoNode.class, TestHippoPublishableDocument.class, TestNewsDocument.class)));

        final TestHippoFolder testHippoFolder = new TestHippoFolder();
        testHippoFolder.setName("testfolder2");

        final TestHippoFolder subfolder = new TestHippoFolder();
        subfolder.setName("subfolder");

        testHippoFolder.add(subfolder);

        final Node node = jcrom.addNode(rootNode, testHippoFolder);
        //assertTrue(node.getNodes().nextNode().getName().equals("subfolder"));


    }
}
