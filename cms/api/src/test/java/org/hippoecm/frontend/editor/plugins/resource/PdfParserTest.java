/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class PdfParserTest {

    @Test
    public void testHandlePdf() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/test.pdf");
            String content = PdfParser.parse(inputStream).trim();
            assertNotNull(content);
            assertEquals(content, "Simple pdf for testing the word : foobarlux");
        } catch (IllegalStateException ex) {
            fail();
        }
    }

    @Test
    public void testPdfLargerThanMaxStringLength() {
        final int originalMaxStringLength = PdfParser.instance.tika.getMaxStringLength();
        final int EXTRACT_LENGTH = 2;
        PdfParser.instance.tika.setMaxStringLength(EXTRACT_LENGTH);
        try {
            InputStream inputStream = getClass().getResourceAsStream("/test.pdf");
            String content = PdfParser.parse(inputStream).trim();
            assertNotNull(content);
            assertEquals(content, "S");
        } catch (IllegalStateException ex) {
            fail();
        } finally {
            // set original value back otherwise other tests might be influenced
            PdfParser.instance.tika.setMaxStringLength(originalMaxStringLength);
        }
    }
}
