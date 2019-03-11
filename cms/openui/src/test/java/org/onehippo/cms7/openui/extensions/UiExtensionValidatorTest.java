/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.openui.extensions;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiExtensionValidatorTest {

    private static final String CHANNEL_PAGE_TOOLS_EXTENSION_POINT = "channel.page.tools";

    private UiExtensionValidator validator;

    @Before
    public void setUp() {
        validator = new UiExtensionValidator();
    }

    @Test
    public void validateId() {
        assertTrue(isValidId("test"));
        assertTrue(isValidId("test1"));
        assertTrue(isValidId("123test"));
        assertTrue(isValidId("myExtension"));
        assertTrue(isValidId("MYEXTENSION"));

        assertFalse(isValidId(null));
        assertFalse(isValidId(""));
        assertFalse(isValidId(" "));
        assertFalse(isValidId("my-extension"));
        assertFalse(isValidId("my.extension"));
        assertFalse(isValidId("my_extension"));
        assertFalse(isValidId("<div></div>"));
        assertFalse(isValidId("\""));
    }

    @Test
    public void validateDisplayName() {
        assertTrue(isValidDisplayName("test"));
        assertTrue(isValidDisplayName("Test"));
        assertTrue(isValidDisplayName("Test Two"));

        assertFalse(isValidDisplayName(null));
        assertFalse(isValidDisplayName(""));
        assertFalse(isValidDisplayName(" "));
    }

    @Test
    public void validateExtensionPoint() {
        assertTrue(isValidExtensionPoint(CHANNEL_PAGE_TOOLS_EXTENSION_POINT));
        assertFalse(isValidExtensionPoint(null));
    }

    @Test
    public void validateUrl() {
        assertTrue(isValidUrl("extensions"));
        assertTrue(isValidUrl("extensions/my-extension"));
        assertTrue(isValidUrl("../extensions"));

        assertFalse(isValidUrl(null));
        assertFalse(isValidUrl(""));
        assertFalse(isValidUrl(" "));
    }

    private boolean isValidId(final String id) {
        return validator.validate(extension(id, id, CHANNEL_PAGE_TOOLS_EXTENSION_POINT, "extensions/" + id));
    }

    private boolean isValidDisplayName(final String displayName) {
        return validator.validate(extension("test", displayName, CHANNEL_PAGE_TOOLS_EXTENSION_POINT, "extensions/test"));
    }

    private boolean isValidExtensionPoint(final String context) {
        return validator.validate(extension("test", "Test", context, "extensions/test"));
    }

    private boolean isValidUrl(final String url) {
        return validator.validate(extension("test", "Test", CHANNEL_PAGE_TOOLS_EXTENSION_POINT, url));
    }

    private UiExtension extension(final String id,
                                  final String displayName,
                                  final String extensionPoint,
                                  final String url) {
        final UiExtensionBean extension = new UiExtensionBean();
        extension.setId(id);
        extension.setDisplayName(displayName);
        extension.setExtensionPoint(extensionPoint);
        extension.setUrl(url);
        return extension;
    }
}
