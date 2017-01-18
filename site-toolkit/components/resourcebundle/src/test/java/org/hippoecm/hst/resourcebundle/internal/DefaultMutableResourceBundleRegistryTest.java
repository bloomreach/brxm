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

import org.hippoecm.hst.resourcebundle.ResourceBundleFamily;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultMutableResourceBundleRegistryTest {

    private static final ResourceBundleFamilyFactory FORBIDDEN_FACTORY = new ResourceBundleFamilyFactory() {
        @Override
        public ResourceBundleFamily createBundleFamily(final String basename) {
            fail("I mustn't be called");
            return null;
        }

        @Override
        public ResourceBundleFamily createBundleFamily(final String basename, final boolean preview) {
            fail("I mustn't be called");
            return null;
        }
    };

    private final Locale locale = new Locale("en");
    private final DefaultMutableResourceBundleRegistry registry = new DefaultMutableResourceBundleRegistry();
    private boolean factoryCalled = false;

    @Test
    public void missingFactoryWithFallback() {
        assertTrue(registry.isFallbackToJavaResourceBundle());
        assertNull(registry.getResourceBundleFamilyFactory());
        try {
            registry.getBundle("basename");
            fail("Should have thrown an exception");
        } catch (MissingResourceException e) {
            assertEquals(e.getStackTrace()[0].getClassName(), "java.util.ResourceBundle");
        }
    }

    @Test
    public void missingFactoryWithFallbackAndLocale() {
        assertTrue(registry.isFallbackToJavaResourceBundle());
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
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry.setResourceBundleFamilyFactory(factoryFor(family, false));

        expect(family.getDefaultBundle()).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundle("basename"), bundle);

        verify(family);
    }

    @Test
    public void defaultBundleForPreview() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry.setResourceBundleFamilyFactory(factoryFor(family, true));

        expect(family.getDefaultBundle()).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundleForPreview("basename"), bundle);

        verify(family);
    }

    @Test
    public void localizedBundle() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry.setResourceBundleFamilyFactory(factoryFor(family, false));

        expect(family.getLocalizedBundle(locale)).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundle("basename", locale), bundle);

        verify(family);
    }

    @Test
    public void localizedBundleForPreview() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry.setResourceBundleFamilyFactory(factoryFor(family, true));

        expect(family.getLocalizedBundle(locale)).andReturn(bundle);

        replay(family);

        assertEquals(registry.getBundleForPreview("basename", locale), bundle);

        verify(family);
    }

    @Test
    public void fallbackToDefaultBundle() {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final ResourceBundleFamily family = createMock(ResourceBundleFamily.class);
        registry.setResourceBundleFamilyFactory(factoryFor(family, false));

        expect(family.getLocalizedBundle(locale)).andReturn(null);
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

    private void validateRegistry(final boolean preview) {
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        final DefaultMutableResourceBundleFamily family = createMock(DefaultMutableResourceBundleFamily.class);

        expect(family.getDefaultBundle()).andReturn(bundle).anyTimes();
        expect(family.getBasename()).andReturn("basename").anyTimes();
        expect(family.getIdentifier()).andReturn("identifier").anyTimes();

        replay(family);

        assertSecondGetBypassesFactory(family, preview);

        // evict bundle globally
        registry.unregisterAllBundleFamilies();

        assertSecondGetBypassesFactory(family, preview);

        // evict bundle by identifier
        registry.unregisterBundleFamily("identifier", preview);

        assertSecondGetBypassesFactory(family, preview);

        // evict bundle by basename
        registry.unregisterBundleFamily("basename");

        assertSecondGetBypassesFactory(family, preview);
    }


    private void validateUnfoundRegistry(final boolean preview) {
        assertSecondGetBypassesFactory(null, preview);

        // wipe unfound registry globally
        registry.unregisterAllBundleFamilies();

        assertSecondGetBypassesFactory(null, preview);

        // wipe unfound registry by identifier
        registry.unregisterBundleFamily("identifier", preview);

        assertSecondGetBypassesFactory(null, preview);

        // wipe unfound registry by basename
        registry.unregisterBundleFamily("basename");

        assertSecondGetBypassesFactory(null, preview);
    }

    private void assertSecondGetBypassesFactory(final ResourceBundleFamily family, final boolean preview) {
        // first load goes into factory
        registry.setResourceBundleFamilyFactory(factoryFor(family, preview));
        loadBundle(family, preview);
        assertTrue(factoryCalled);

        // second load should not go onto factory
        registry.setResourceBundleFamilyFactory(FORBIDDEN_FACTORY);
        loadBundle(family, preview);
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
        return new ResourceBundleFamilyFactory() {
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
