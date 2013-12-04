/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * Tests {@link CKEditorConstants}
 */
public class CKEditorConstantsTest {

    @Test
    public void ckeditorJsConstantRefersToFileInJar() {
        assertConstantRefersToFileInJar(CKEditorConstants.CKEDITOR_OPTIMIZED_JS);
    }

    @Test
    public void ckeditorSrcJsConstantRefersToFileInJar() {
        assertConstantRefersToFileInJar(CKEditorConstants.CKEDITOR_SRC_JS);
    }

    private static void assertConstantRefersToFileInJar(final ResourceReference constant) {
        final String url = ((UrlResourceReference)constant).getUrl().toString();
        final InputStream js = CKEditorConstants.class.getResourceAsStream("/" + url);
        assertNotNull("The file '" + url + "' does not exist in the same JAR", js);
        try {
            js.close();
        } catch (IOException e) {
            // ignore
        }
    }

}
