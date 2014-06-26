package org.onehippo.cms7.essentials.plugins.taxonomy;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

import static junit.framework.Assert.*;

/**
 * Created by mrop on 26-6-14.
 */
public class ClassificationDaoServiceBuilderTest {

    public static final String TAXONOMYDEMO_ALTERNATEKEYS = "taxonomydemo:alternatekeys";
    public static final String ALTERNATE_CLASSIFICATION_DAO_SERVICE = "alternateClassificationDaoService";
    public static final String SERVICE_TAXONOMY_DAO_ALTERNATE = "service.taxonomy.dao.alternate";
    private MockSession mockSession;

    @Before
    public void setUp() throws Exception {
        MockNode root = new MockNode(null,"rep:root");
        MockNode configuration = root.addMockNode("hippo:configuration", "hipposys:configuration");
        MockNode frontend = configuration.addMockNode("hippo:frontend", "hipposys:applicationfolder");
        MockNode cms = frontend.addMockNode("cms", "frontend:application");
        cms.addMockNode("cms-services", "frontend:plugincluster");
        mockSession = new MockSession(root);

    }

    @Test
    public void testBuild_fieldPath() throws Exception {
        ClassificationDaoServiceBuilder ClassificationDaoServiceBuilder = new ClassificationDaoServiceBuilder();
        ClassificationDaoServiceBuilder.setFieldPath(TAXONOMYDEMO_ALTERNATEKEYS);
        ClassificationDaoServiceBuilder.setTaxonomyClassificationDao("service.taxonomy.dao.alternate");
        ClassificationDaoServiceBuilder.setClassificationDaoServiceName("alternateClassificationDaoService");
        ClassificationDaoServiceBuilder.setSession(mockSession);
        ClassificationDaoServiceBuilder.build();
        Node actualNode = ClassificationDaoServiceBuilder.getNode();
        String actualFieldPath = actualNode.getProperty("fieldPath").getValue().getString();
        assertEquals(TAXONOMYDEMO_ALTERNATEKEYS, actualFieldPath);
    }

    @Test
    public void testBuild_taxonomyClassificationDao() throws Exception {
        ClassificationDaoServiceBuilder ClassificationDaoServiceBuilder = new ClassificationDaoServiceBuilder();
        ClassificationDaoServiceBuilder.setFieldPath(TAXONOMYDEMO_ALTERNATEKEYS);
        ClassificationDaoServiceBuilder.setTaxonomyClassificationDao(SERVICE_TAXONOMY_DAO_ALTERNATE);
        ClassificationDaoServiceBuilder.setClassificationDaoServiceName(ALTERNATE_CLASSIFICATION_DAO_SERVICE);
        ClassificationDaoServiceBuilder.setSession(mockSession);
        ClassificationDaoServiceBuilder.build();
        Node actualNode = ClassificationDaoServiceBuilder.getNode();
        String actual = actualNode.getProperty("taxonomy.classification.dao").getValue().getString();
        assertEquals(SERVICE_TAXONOMY_DAO_ALTERNATE,actual);
    }

    @Test
    public void testBuild_name() throws Exception {
        ClassificationDaoServiceBuilder ClassificationDaoServiceBuilder = new ClassificationDaoServiceBuilder();
        ClassificationDaoServiceBuilder.setFieldPath(TAXONOMYDEMO_ALTERNATEKEYS);
        ClassificationDaoServiceBuilder.setTaxonomyClassificationDao(SERVICE_TAXONOMY_DAO_ALTERNATE);

        ClassificationDaoServiceBuilder.setClassificationDaoServiceName(ALTERNATE_CLASSIFICATION_DAO_SERVICE);
        ClassificationDaoServiceBuilder.setSession(mockSession);
        ClassificationDaoServiceBuilder.build();
        Node actualNode = ClassificationDaoServiceBuilder.getNode();
        String actual = actualNode.getName();
        assertEquals(ALTERNATE_CLASSIFICATION_DAO_SERVICE,actual);
    }
}
