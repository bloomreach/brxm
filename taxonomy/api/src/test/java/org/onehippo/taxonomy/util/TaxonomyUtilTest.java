/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.taxonomy.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

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

        // Util is lenient for locale strings with underscores. Variant "11" is condoned.
        Locale enGB11 = new Locale("en", "GB", "11");
        assertEquals(TaxonomyUtil.toLocale("en_GB_11"), enGB11);

        // Util is not lenient for languageTags. Variant "11" is invalid.
        assertNull(TaxonomyUtil.toLocale("en-GB-11"));

        // LanguageTags with a valid variant will produce a valid Locale object.
        Locale enGB0011 = new Locale("en", "GB", "0011");
        assertEquals(TaxonomyUtil.toLocale("en-GB-0011"), enGB0011);
        Locale enGBHIP11 = new Locale("en", "GB", "HIP11");
        assertEquals(TaxonomyUtil.toLocale("en-GB-HIP11"), enGBHIP11);
    }

    @Test
    public void getLocalesList_filterPrototypeLocale() {
        final List<Locale> localesList = TaxonomyUtil.getLocalesList(new String[]{"document-type-locale"});
        assertEquals(Collections.emptyList(), localesList);
    }
}
