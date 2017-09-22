package org.onehippo.taxonomy.util;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.onehippo.taxonomy.api.Taxonomy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaxonomyUtilTest {

    @Test
    public void getLocalesListTest () {
        final List<Locale> localesList = TaxonomyUtil.getLocalesList(null);
        assertNotNull("TaxonomyUtil may not return null.", localesList);
    }

    @Test
    public void toLocaleTest () {
        assertNull(TaxonomyUtil.toLocale(null));
        assertEquals(TaxonomyUtil.toLocale("en_GB"), Locale.UK);
        assertEquals(TaxonomyUtil.toLocale("en-GB"), Locale.UK);
    }
}
