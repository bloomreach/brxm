/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.resourcebundle.internal;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.SimpleListResourceBundle;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResourceBundleRegistryImplTest {

    private static final String BASE_NAME = ResourceBundleRegistryImplTest.class.getPackage().getName() + ".TestMessages";
    private final Locale locale = new Locale("en");
    private ResourceBundleRegistryImpl registry = new ResourceBundleRegistryImpl(
            new ResourceBundleFamilyFactory(null, null, null) {
                @Override
                public ResourceBundleFamily createBundleFamily(final String basename, final boolean preview) {
                    return null;
                }
            });
    private boolean factoryCalled = false;

    @Test
    public void fallbackToJavaResourceBundle() {
        try {
            registry.getBundle("basename");
            fail("Should have thrown an exception");
        } catch (MissingResourceException e) {
            assertEquals(e.getStackTrace()[0].getClassName(), "java.util.ResourceBundle");
        }
    }

    @Test
    public void fallbackToJavaResourceBundleWithLocale() {
        try {
            registry.getBundle("basename", locale);
            fail("Should have thrown an exception");
        } catch (MissingResourceException e) {
            assertEquals(e.getClassName(), "basename_en");
        }
    }

    @Test
    public void missingFactoryNoFallback() {
        try {
            registry.setFallbackToJavaResourceBundle(false);
            registry.getBundle("basename");
            fail("Should have thrown an exception");
        } catch (MissingResourceException e) {
            assertEquals(e.getClassName(), "basename");
        }
    }

    @Test
    public void defaultBundle() {
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        registry = new ResourceBundleRegistryImpl(factoryFor(family, false));

        expect(family.getVariantUUID()).andReturn("identifier");
        expect(family.getDefaultBundle()).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundle("basename"), bundle);

        verify(family);
    }

    @Test
    public void defaultBundleForPreview() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry = new ResourceBundleRegistryImpl(factoryFor(family, true));

        expect(family.getVariantUUID()).andReturn("identifier");
        expect(family.getDefaultBundle()).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundleForPreview("basename"), bundle);

        verify(family);
    }

    @Test
    public void localizedBundle() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry = new ResourceBundleRegistryImpl(factoryFor(family, false));

        expect(family.getVariantUUID()).andReturn("identifier");
        expect(family.getLocalizedBundle(locale)).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundle("basename", locale), bundle);

        verify(family);
    }

    @Test
    public void localizedBundleForPreview() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry = new ResourceBundleRegistryImpl(factoryFor(family, true));

        expect(family.getVariantUUID()).andReturn("identifier");
        expect(family.getLocalizedBundle(locale)).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundleForPreview("basename", locale), bundle);

        verify(family);
    }

    @Test
    public void fallbackToDefaultBundle() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry = new ResourceBundleRegistryImpl(factoryFor(family, false));

        expect(family.getLocalizedBundle(locale)).andReturn(null);
        expect(family.getVariantUUID()).andReturn("identifier");
        expect(family.getDefaultBundle()).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundle("basename", locale), bundle);

        verify(family);
    }

    @Test
    public void unfoundBundle() {
        validateUnfoundRegistry(false);
    }

    @Test
    public void unfoundBundleForPreview() {
        validateUnfoundRegistry(true);
    }

    @Test
    public void bundleByBasename() {
        validateRegistry(false);
    }

    @Test
    public void bundleByBasenameForPreview() {
        validateRegistry(true);
    }

    @Test
    public void testBundlesFromJava() throws Exception {
        Locale locale = LocaleUtils.toLocale("en");
        ResourceBundle bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Name (en)", bundle.getString("name"));
        Assert.assertEquals("Hello (en)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_US");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Name (en_US)", bundle.getString("name"));
        Assert.assertEquals("Hello (en_US)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_CA");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Name (en_CA)", bundle.getString("name"));
        Assert.assertEquals("Hello (en_CA)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Nom (fr)", bundle.getString("name"));
        Assert.assertEquals("Bonjour (fr)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_FR");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Nom (fr_FR)", bundle.getString("name"));
        Assert.assertEquals("Bonjour (fr_FR)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_CA");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("Nom (fr_CA)", bundle.getString("name"));
        Assert.assertEquals("Bonjour (fr_CA)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));
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

        registry = new ResourceBundleRegistryImpl(new ResourceBundleFamilyFactory(null, null, null) {
            @Override
            public ResourceBundleFamily createBundleFamily(String basename) {
                ResourceBundleFamily family = new ResourceBundleFamily(BASE_NAME);

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

            @Override
            public ResourceBundleFamily createBundleFamily(String basename, boolean preview) {
                return createBundleFamily(basename);
            }
        });

        Locale locale = LocaleUtils.toLocale("en");
        ResourceBundle bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NAME (en)", bundle.getString("name"));
        Assert.assertEquals("HELLO (en)", bundle.getString("greeting"));
        // default bundle has 'missing' key
        Assert.assertEquals("default", bundle.getString("missing"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_US");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NAME (en_US)", bundle.getString("name"));
        Assert.assertEquals("HELLO (en_US)", bundle.getString("greeting"));
        // en_US has 'missing' key but with value [<missing>] which means missing and thus bubbles up
        Assert.assertEquals("default", bundle.getString("missing"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("en_CA");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NAME (en)", bundle.getString("name"));
        Assert.assertEquals("HELLO (en)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NOM (fr)", bundle.getString("name"));
        Assert.assertEquals("BONJOUR (fr)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_FR");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NOM (fr_FR)", bundle.getString("name"));
        Assert.assertEquals("BONJOUR (fr_FR)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("fr_CA");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NOM (fr)", bundle.getString("name"));
        Assert.assertEquals("BONJOUR (fr)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));

        locale = LocaleUtils.toLocale("it_IT");
        bundle = registry.getBundle(BASE_NAME, locale);
        assertNotNull(bundle);
        Assert.assertEquals("simple bundle NAME (default)", bundle.getString("name"));
        Assert.assertEquals("HELLO (default)", bundle.getString("greeting"));
        assertSame(bundle, registry.getBundle(BASE_NAME, locale));
    }

    private void validateRegistry(final boolean preview) {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);

        expect(family.getDefaultBundle()).andReturn(bundle).anyTimes();
        expect(family.getBasename()).andReturn("basename").anyTimes();
        expect(family.getVariantUUID()).andReturn("identifier").anyTimes();

        replay(family);

        assertSecondGetBypassesFactory(family, preview);

        // evict bundle by identifier
        registry.unregisterBundleFamily("identifier", preview);

        assertSecondGetBypassesFactory(family, preview);
    }


    private void validateUnfoundRegistry(final boolean preview) {
        assertSecondGetBypassesFactory(null, preview);

        // wipe unfound registry by identifier
        registry.unregisterBundleFamily("identifier", preview);

        assertSecondGetBypassesFactory(null, preview);
    }

    private void assertSecondGetBypassesFactory(final ResourceBundleFamily family, final boolean preview) {
        // first load goes into factory
        registry = new ResourceBundleRegistryImpl(factoryFor(family, preview));
        loadBundle(family, preview);
        assertTrue(factoryCalled);

        // second load should not go onto factory
        factoryCalled = false;
        loadBundle(family, preview);
        assertFalse(factoryCalled);
    }

    private void loadBundle(final ResourceBundleFamily family, final boolean preview) {
        try {
            final ResourceBundle bundle = preview ? registry.getBundleForPreview("basename") : registry.getBundle("basename");
            if (family == null) {
                fail("Should throw an exception");
            } else {
                assertEquals(bundle, family.getDefaultBundle());
            }
        } catch (MissingResourceException ignored) { }
    }

    private ResourceBundleFamilyFactory factoryFor(final ResourceBundleFamily family, final boolean expectedPreview) {
        factoryCalled = false;
        return new ResourceBundleFamilyFactory(null, null, null) {
            @Override
            public ResourceBundleFamily createBundleFamily(String basename, boolean preview) {
                factoryCalled = true;
                return preview == expectedPreview ? family : null;
            }

            @Override
            public ResourceBundleFamily createBundleFamily(String basename) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
