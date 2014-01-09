/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.repository.api.Localized;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link RenameMessage}.
 */
public class RenameMessageTest extends PluginTest {

    private Map<Localized, String> localizedNames;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        localizedNames = new HashMap<Localized, String>();
    }

    private void addDefaultLocalizedName(final String name) {
        localizedNames.put(Localized.getInstance(), name);
    }

    private void addLocalizedName(final String language, final String name) {
        final Locale locale = LocaleUtils.toLocale(language);
        localizedNames.put(Localized.getInstance(locale), name);
    }

    @Test
    public void noLocalizedNames() {
        final RenameMessage message = new RenameMessage(Locale.ENGLISH, localizedNames);
        assertFalse(message.shouldShow());
    }

    @Test
    public void onlyDefault() {
        addDefaultLocalizedName("Test");
        final RenameMessage message = new RenameMessage(Locale.ENGLISH, localizedNames);
        assertFalse(message.shouldShow());
    }

    @Test
    public void defaultAndMatching() {
        addDefaultLocalizedName("Test");
        addLocalizedName("en", "Test EN");
        final RenameMessage message = new RenameMessage(Locale.ENGLISH, localizedNames);
        assertTrue(message.shouldShow());
        assertEquals("This document has the following localized names: for locale \"default\" the name \"Test\", for locale \"en\" the name \"Test EN\". The new name will replace all existing localized names.", message.forDocument());
        assertEquals("This folder has the following localized names: for locale \"default\" the name \"Test\", for locale \"en\" the name \"Test EN\". The new name will replace all existing localized names.", message.forFolder());
    }

    @Test
    public void defaultAndNonMatching() {
        addDefaultLocalizedName("Test");
        addLocalizedName("en", "Test EN");
        final RenameMessage message = new RenameMessage(Locale.FRENCH, localizedNames);
        assertTrue(message.shouldShow());
        assertEquals("This document has the following localized names: for locale \"default\" the name \"Test\", for locale \"en\" the name \"Test EN\". The new name will replace all existing localized names.", message.forDocument());
        assertEquals("This folder has the following localized names: for locale \"default\" the name \"Test\", for locale \"en\" the name \"Test EN\". The new name will replace all existing localized names.", message.forFolder());
    }

    @Test
    public void onlyMatchingLocalized() {
        addLocalizedName("en", "Test EN");
        final RenameMessage message = new RenameMessage(Locale.ENGLISH, localizedNames);
        assertFalse(message.shouldShow());
    }

    @Test
    public void onlyNonMatchingLocalized() {
        addLocalizedName("fr", "Test FR");
        final RenameMessage message = new RenameMessage(Locale.ENGLISH, localizedNames);
        assertTrue(message.shouldShow());
        assertEquals("This document has the following localized names: for locale \"fr\" the name \"Test FR\". The new name will replace all existing localized names.", message.forDocument());
        assertEquals("This folder has the following localized names: for locale \"fr\" the name \"Test FR\". The new name will replace all existing localized names.", message.forFolder());
    }

}
