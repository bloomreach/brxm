package org.onehippo.cms7.essentials.plugins.taxonomy;

import javax.jcr.Node;
import javax.jcr.Property;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

import junit.framework.Assert;

/**
 * Created by mrop on 26-6-14.
 */
public class AdditionalTaxonomyBuilderTest {

    public static final String SERVICE_TAXONOMY_DAO_ALTERNATE = "service.taxonomy.dao.alternate";
    private MockSession mockSession;

    @Before
    public void setUp() throws Exception {
        MockNode root = new MockNode(null,"rep:root");
        MockNode namespaces = root.addMockNode("hippo:namespaces", "hipposysedit:namespacefolder");
        MockNode gettingstarted = namespaces.addMockNode("gettingstarted", "hipposysedit:namespace");
        MockNode documentType = gettingstarted.addMockNode("newsdocument", "hipposysedit:templatetype");
        MockNode templates = documentType.addMockNode("editor:templates", "editor:templateset");
        templates.addMockNode("_default_","frontend:plugincluster");
        mockSession = new MockSession(root);

    }

    @Test
    public void testBuild_name() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        String expected = "exampletaxonomy";
        builder.build();
        Node actualNode = builder.getNode();
        String actual = actualNode.getName();
        Assert.assertEquals(expected,actual);
    }

    private AdditionalTaxonomyBuilder initBuilder() {
        AdditionalTaxonomyBuilder builder = new AdditionalTaxonomyBuilder();
        builder.setSession(mockSession);
        builder.setTaxonomyName("exampletaxonomy");
        builder.setPrefix("gettingstarted");
        builder.setDocumentType("newsdocument");
        return builder;
    }

    @Test
    public void testBuild_mode() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();
        Node actualNode = builder.getNode();
        String expected = "${mode}";
        String actual = actualNode.getProperty("mode").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_modeCompareTo() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();
        Node actualNode = builder.getNode();
        String expected = "${model.compareTo}";
        String actual = actualNode.getProperty("model.compareTo").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_pluginClass() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();
        Node actualNode = builder.getNode();
        String expected = "org.onehippo.taxonomy.plugin.TaxonomyPickerPlugin";
        String actual = actualNode.getProperty("plugin.class").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_taxonomyClassificationDao() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.setTaxonomyClassificationDao(SERVICE_TAXONOMY_DAO_ALTERNATE);
        builder.build();
        Node actualNode = builder.getNode();

        String expected = SERVICE_TAXONOMY_DAO_ALTERNATE;
        String actual = actualNode.getProperty("taxonomy.classification.dao").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_taxonomyId() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();
        Node actualNode = builder.getNode();

        String expected = "service.taxonomy";
        String actual = actualNode.getProperty("taxonomy.id").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_taxonomyName() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();

        Node actualNode = builder.getNode();

        String expected = "exampletaxonomy";
        String actual = actualNode.getProperty("taxonomy.name").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_wicketId() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();

        Node actualNode = builder.getNode();

        String expected = "${cluster.id}.left.item";
        String actual = actualNode.getProperty("wicket.id").getString();
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testBuild_wicketModel() throws Exception {
        AdditionalTaxonomyBuilder builder = initBuilder();
        builder.build();

        Node actualNode = builder.getNode();

        String expected = "${wicket.model}";
        String actual = actualNode.getProperty("wicket.model").getString();
        Assert.assertEquals(expected,actual);
    }
}
