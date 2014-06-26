package org.onehippo.cms7.essentials.plugins.taxonomy;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import junit.framework.Assert;

/**
 * Created by mrop on 26-6-14.
 */
public class ServiceNameBuilderTest {
    @Test
    public void testGetServiceName() throws Exception {
        String actual = ServiceNameBuilder.getServiceName("gettingstarted:newsdocument", "exampletaxonomy");
        String expected = "taxonomyclassificationnewsdocumentexampletaxonomy";
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void testGetAdditionalTaxonomyNodeName() throws Exception{
        String actual = ServiceNameBuilder.getAdditionalTaxonomyNodeName("gettingstarted","newsdocument", "exampletaxonomy");
        String expected = "/hippo:namespaces/gettingstarted/newsdocument/editor:templates/_default_/exampletaxonomy";
        Assert.assertEquals(expected,actual);
    }


}
