/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.internal.DefaultMutableResourceBundleFamily;
import org.hippoecm.hst.resourcebundle.internal.DefaultMutableResourceBundleRegistry;
import org.hippoecm.hst.resourcebundle.internal.ResourceBundleFamilyFactory;
import org.junit.Test;

/**
 * TestDefaultResourceBundleRegistry
 */
public class TestDefaultResourceBundleRegistry {

    private static final String BASE_NAME = TestDefaultResourceBundleRegistry.class.getPackage().getName() + ".TestMessages";

    private DefaultMutableResourceBundleRegistry resourceBundleRegistry = new DefaultMutableResourceBundleRegistry();

    @Test
    public void testBundlesFromJava() throws Exception {
        Locale locale = LocaleUtils.toLocale("en");
        ResourceBundle bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Name (en)", bundle.getString("name"));
        assertEquals("Hello (en)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_US");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Name (en_US)", bundle.getString("name"));
        assertEquals("Hello (en_US)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_CA");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Name (en_CA)", bundle.getString("name"));
        assertEquals("Hello (en_CA)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Nom (fr)", bundle.getString("name"));
        assertEquals("Bonjour (fr)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_FR");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Nom (fr_FR)", bundle.getString("name"));
        assertEquals("Bonjour (fr_FR)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_CA");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("Nom (fr_CA)", bundle.getString("name"));
        assertEquals("Bonjour (fr_CA)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));
    }

    @Test
    public void testBundlesWithResourceBundleFamilyFactory() throws Exception {
        final ResourceBundle defaultBundle = new SimpleListResourceBundle(ImmutableMap.of(
                "name", "simple bundle NAME (default)",
                "greeting", "HELLO (default)",
                "missing", "default"
        ));
        final SimpleListResourceBundle enBundle = new SimpleListResourceBundle(ImmutableMap.of(
                "name", "simple bundle NAME (en)",
                "greeting", "HELLO (en)"
        ));
        enBundle.setParent(defaultBundle);

        final SimpleListResourceBundle enUSBundle = new SimpleListResourceBundle(ImmutableMap.of(
                "name", "simple bundle NAME (en_US)",
                "greeting", "HELLO (en_US)",
                "missing", "[<missing>]"
        ));
        enUSBundle.setParent(enBundle);


        resourceBundleRegistry.setResourceBundleFamilyFactory(new ResourceBundleFamilyFactory() {
            @Override
            public ResourceBundleFamily createBundleFamily(String basename) {
                DefaultMutableResourceBundleFamily family = new DefaultMutableResourceBundleFamily(BASE_NAME);

                family.setDefaultBundle(defaultBundle);
                family.setLocalizedBundle(LocaleUtils.toLocale("en"),enBundle);
                family.setLocalizedBundle(LocaleUtils.toLocale("en_US"),enUSBundle);
                family.setLocalizedBundle(LocaleUtils.toLocale("fr"),
                        new SimpleListResourceBundle(ImmutableMap.of(
                                "name", "simple bundle NOM (fr)",
                                "greeting", "BONJOUR (fr)"
                        )));
                family.setLocalizedBundle(LocaleUtils.toLocale("fr_FR"),
                        new SimpleListResourceBundle(ImmutableMap.of(
                                "name", "simple bundle NOM (fr_FR)",
                                "greeting", "BONJOUR (fr_FR)"
                        )));
                return family;
            }
        });

        Locale locale = LocaleUtils.toLocale("en");
        ResourceBundle bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NAME (en)", bundle.getString("name"));
        assertEquals("HELLO (en)", bundle.getString("greeting"));
        // default bundle has 'missing' key
        assertEquals("default", bundle.getString("missing"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_US");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NAME (en_US)", bundle.getString("name"));
        assertEquals("HELLO (en_US)", bundle.getString("greeting"));
        // en_US has 'missing' key but with value [<missing>] which means missing and thus bubbles up
        assertEquals("default", bundle.getString("missing"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_CA");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NAME (en)", bundle.getString("name"));
        assertEquals("HELLO (en)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NOM (fr)", bundle.getString("name"));
        assertEquals("BONJOUR (fr)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_FR");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NOM (fr_FR)", bundle.getString("name"));
        assertEquals("BONJOUR (fr_FR)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_CA");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NOM (fr)", bundle.getString("name"));
        assertEquals("BONJOUR (fr)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("it_IT");
        bundle = resourceBundleRegistry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        assertEquals("simple bundle NAME (default)", bundle.getString("name"));
        assertEquals("HELLO (default)", bundle.getString("greeting"));
        assertSame(bundle, resourceBundleRegistry.getBundle(BASE_NAME, locale));
    }

}
