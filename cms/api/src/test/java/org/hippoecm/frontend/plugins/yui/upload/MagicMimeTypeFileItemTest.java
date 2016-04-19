package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.tika.mime.MediaType;
import org.apache.wicket.util.file.FileCleaner;
import org.apache.wicket.util.upload.DiskFileItem;
import org.apache.wicket.util.upload.FileItem;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

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
        TestFileItem testFileItem = new TestFileItem(mediaType, stream, TEST_ODS);
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
    }

}