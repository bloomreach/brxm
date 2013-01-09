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

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PdfParserTest {

    @Test
    public void testHandlePdf() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/test.pdf");
            String content = PdfParser.synchronizedParse(inputStream);
            assertNotNull(content);
            System.out.println(content);
            assertTrue(content.startsWith("Simple pdf for testing the word : foobarlux"));
        } catch (IllegalStateException ex) {
            fail();
        }
    }

    @Test
    public void testPdfLargerThanTikeMaxStringLenght() {
        final int orginalTikaLength = PdfParser.pdfParser.tika.getMaxStringLength();
        final int EXTRACT_LENGTH = 1;
        PdfParser.pdfParser.tika.setMaxStringLength(EXTRACT_LENGTH);
        try {
            InputStream inputStream = getClass().getResourceAsStream("/test.pdf");
            String content = PdfParser.synchronizedParse(inputStream);
            assertNotNull(content);
            assertTrue(content.equals("S"));
        } catch (IllegalStateException ex) {
            fail();
        } finally {
            // set original value back otherwise other tests might be influenced
            PdfParser.pdfParser.tika.setMaxStringLength(orginalTikaLength);
        }
    }

    @Test(expected = TikaException.class)
    public void testDeprecateTikaParseUtilsThrowTikaException() throws TikaException, IOException {
        int s = PdfParser.pdfParser.tika.getMaxStringLength();
        InputStream inputStream = getClass().getResourceAsStream("/test.pdf");
        getStringContent(inputStream, TikaConfig.getDefaultConfig(), ResourceHelper.MIME_TYPE_PDF);
    }

    /**
     * see {@link org.apache.tika.utils.ParseUtils#getStringContent(java.io.InputStream, org.apache.tika.config.TikaConfig, String)} only
     * below we force the bosy content handler to have a writeLimit of just one char. Then, the deprecated Tika ParseUtil
     * will throw a TikaException exception.
     */
    public static String getStringContent(
            InputStream stream, TikaConfig config, String mimeType)
            throws TikaException, IOException {
        try {
            Parser parser = config.getParser(MediaType.parse(mimeType));
            ContentHandler handler = new BodyContentHandler(1);
            parser.parse(stream, handler, new Metadata());
            return handler.toString();
        } catch (SAXException e) {
            throw new TikaException("Unexpected SAX error", e);
        }
    }


}
