/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.ckeditor;

import org.apache.wicket.request.resource.UrlResourceReference;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Tests {@link CKEditorConstants}
 */
public class CKEditorConstantsTest {

    @Test
    public void ckeditorJsConstantRefersToFileOnClassPath() {
        assertConstantRefersToFileOnClassPath(CKEditorConstants.CKEDITOR_OPTIMIZED_JS);
    }

    private static void assertConstantRefersToFileOnClassPath(final UrlResourceReference ref) {
        final String path = ref.setContextRelative(false).getUrl().getPath();
        assertTrue("The file '" + path + "' does not exist on the classpath", CKEditorConstants.existsOnClassPath(ref));
    }
}
