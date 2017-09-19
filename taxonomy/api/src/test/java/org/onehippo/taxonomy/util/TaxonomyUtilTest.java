package org.onehippo.taxonomy.util;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TaxonomyUtilTest {

    @Test
    public void getLocalesList () {
        final List<Locale> localesList = TaxonomyUtil.getLocalesList(null);
        assertNotNull("TaxonomyUtil may not return null.", localesList);
    }

}
