/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class MagicMimeTypeFileItemTest {

    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT = "application/vnd.oasis.opendocument.text";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
    public static final String TEST_ODT = "test.odt";
    public static final String TEST_ODS = "test.ods";


    /**
     * https://issues.onehippo.com/browse/CMS-10040
     */
    @Test
    public void testContentTypeOpenDocumentText() {
        MagicMimeTypeFileItem magicMimeTypeFileItem = setUpMagic(TEST_ODT, APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT);

        String contentType = magicMimeTypeFileItem.getContentType();

        assertEquals(APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT, contentType);
    }

    /**
     * https://issues.onehippo.com/browse/CMS-10040
     */
    @Test
    public void testContentTypeOpenDocumentSpreadsheet() {
        MagicMimeTypeFileItem magicMimeTypeFileItem = setUpMagic(TEST_ODS, APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET);

        String contentType = magicMimeTypeFileItem.getContentType();

        assertEquals(APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET, contentType);
    }


    private MagicMimeTypeFileItem setUpMagic(String fileName, String mediaType) {
        InputStream stream = getClass().getResourceAsStream(fileName);
        TestFileItem testFileItem = new TestFileItem(mediaType, stream, fileName);
        return new MagicMimeTypeFileItem(testFileItem);
    }

    public class TestFileItem implements FileItem {
        private String contentType;
        private InputStream inputStream;
        private String fileName;

        public TestFileItem(String contentType, InputStream inputStream, String fileName) {
            this.contentType = contentType;
            this.inputStream = inputStream;
            this.fileName = fileName;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public boolean isInMemory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(String encoding) throws UnsupportedEncodingException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(File file) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFieldName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFieldName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFormField() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFormField(boolean state) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileItemHeaders getHeaders() {
            return null;
        }

        @Override
        public void setHeaders(final FileItemHeaders headers) {
        }
    }

}