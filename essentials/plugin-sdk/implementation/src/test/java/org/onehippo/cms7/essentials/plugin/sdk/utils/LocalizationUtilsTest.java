/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.util.Locale;

import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

import static org.junit.Assert.assertEquals;

public class LocalizationUtilsTest {

    private static final Class<LocalizationService> LS = LocalizationService.class;
    private LocalizationService testLocalizationService;

    @Test
    public void get_content_type_display_name_no_service() {
        assertEquals("foo", LocalizationUtils.getContentTypeDisplayName("foo"));
    }

    @Test
    public void get_content_type_display_name_no_bundle() {
        final TestLocalizationService ls = new TestLocalizationService();
        setLocalizationService(ls);
        assertEquals("foo", LocalizationUtils.getContentTypeDisplayName("foo"));
        assertEquals("hippo:types.hipposys:foo", ls.bundleName);
        assertEquals(Locale.ENGLISH, ls.locale);
    }

    @Test
    public void get_content_type_display_name_blank() {
        final TestLocalizationService ls = new TestLocalizationService();
        final TestResourceBundle rb = new TestResourceBundle();
        rb.value = " "; // blank
        ls.resourceBundle = rb;
        setLocalizationService(ls);

        assertEquals("foo", LocalizationUtils.getContentTypeDisplayName("foo"));
        assertEquals("jcr:name", rb.key);
    }

    @Test
    public void get_content_type_display_name() {
        final TestLocalizationService ls = new TestLocalizationService();
        final TestResourceBundle rb = new TestResourceBundle();
        rb.value = "bar";
        ls.resourceBundle = rb;
        setLocalizationService(ls);

        assertEquals("bar", LocalizationUtils.getContentTypeDisplayName("foo:baz"));
        assertEquals("hippo:types.foo:baz", ls.bundleName);
    }

    private void setLocalizationService(final LocalizationService ls) {
        testLocalizationService = ls;
        HippoServiceRegistry.registerService(testLocalizationService, LS);
    }

    @After
    public void resetLocalizationService() {
        if (testLocalizationService != null) {
            HippoServiceRegistry.unregisterService(testLocalizationService, LS);
            testLocalizationService = null;
        }
    }

    private static class TestLocalizationService implements LocalizationService {
        ResourceBundle resourceBundle;
        String bundleName;
        Locale locale;

        @Override
        public ResourceBundle getResourceBundle(final String bundleName, final Locale locale) {
            this.bundleName = bundleName;
            this.locale = locale;

            return resourceBundle;
        }
    }

    private static class TestResourceBundle implements ResourceBundle {
        String key;
        String value;

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getString(final String key) {
            this.key = key;
            return value;
        }
    }
}
