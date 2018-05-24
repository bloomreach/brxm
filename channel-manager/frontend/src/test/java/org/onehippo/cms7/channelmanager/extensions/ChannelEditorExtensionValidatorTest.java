/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.extensions;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChannelEditorExtensionValidatorTest {

    private ChannelEditorExtensionValidator validator;

    @Before
    public void setUp() {
        validator = new ChannelEditorExtensionValidator();
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
    public void validateContext() {
        assertTrue(isValidContext(CmsExtensionContext.PAGE));
        assertFalse(isValidContext(null));
    }

    @Test
    public void validateUrlPath() {
        assertTrue(isValidUrlPath("extensions"));
        assertTrue(isValidUrlPath("extensions/my-extension"));
        assertTrue(isValidUrlPath("../extensions"));

        assertFalse(isValidUrlPath(null));
        assertFalse(isValidUrlPath(""));
        assertFalse(isValidUrlPath(" "));
    }

    private boolean isValidId(final String id) {
        return validator.validate(extension(id, id, CmsExtensionContext.PAGE, "extensions/" + id));
    }

    private boolean isValidDisplayName(final String displayName) {
        return validator.validate(extension("test", displayName, CmsExtensionContext.PAGE, "extensions/test"));
    }

    private boolean isValidContext(final CmsExtensionContext context) {
        return validator.validate(extension("test", "Test", context, "extensions/test"));
    }

    private boolean isValidUrlPath(final String urlPath) {
        return validator.validate(extension("test", "Test", CmsExtensionContext.PAGE, urlPath));
    }

    private CmsExtension extension(final String id,
                                   final String displayName,
                                   final CmsExtensionContext context,
                                   final String urlPath) {
        final CmsExtensionBean extension = new CmsExtensionBean();
        extension.setId(id);
        extension.setDisplayName(displayName);
        extension.setContext(context);
        extension.setUrlPath(urlPath);
        return extension;
    }
}