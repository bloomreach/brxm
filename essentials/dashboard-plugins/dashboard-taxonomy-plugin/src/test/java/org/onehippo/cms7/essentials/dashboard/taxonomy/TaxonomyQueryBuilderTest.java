package org.onehippo.cms7.essentials.dashboard.taxonomy;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.taxonomy.util.TaxonomyQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


/**
 * @version "$Id$"
 */
public class TaxonomyQueryBuilderTest {

    private static Logger log = LoggerFactory.getLogger(TaxonomyQueryBuilderTest.class);

    @Test
    public void testTaxonomyQueryBuilder() throws Exception {
        TaxonomyQueryBuilder builder = new TaxonomyQueryBuilder.Builder().addDocumentType("hippoplugins:newsdocument").addDocumentType("hippoplugins:textdocument").build();
        assertEquals(builder.getQuery(), "//element(*,hippo:document)[jcr:primaryType='hippoplugins:newsdocument' or jcr:primaryType='hippoplugins:textdocument' and not(jcr:mixinTypes='hippotaxonomy:classifiable')]");
    }
}
