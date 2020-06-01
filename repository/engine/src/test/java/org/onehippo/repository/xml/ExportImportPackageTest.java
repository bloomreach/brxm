/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.ValueFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.FileContentResourceLoader;
import org.onehippo.repository.util.ZipFileContentResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportImportPackageTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(ExportImportPackageTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session.getRootNode().addNode("test");
        final ValueFactory valueFactory = session.getValueFactory();
        final Binary binary = valueFactory.createBinary(new ByteArrayInputStream("test".getBytes()));
        test.setProperty("test", valueFactory.createValue(binary));
        session.save();
    }

    @Test
    public void testExportImportPackage() throws Exception {
        HippoSession session = (HippoSession) this.session;
        final File file = session.exportEnhancedSystemViewPackage("/test", true);
        ZipFile zipFile = new ZipFile(file);
        InputStream esvIn = null;
        try {
            List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
            assertEquals(2, entries.size()); // esv.xml and one binary
            ZipEntry esvXmlEntry = null;
            ZipEntry binaryEntry = null;
            for (ZipEntry entry : entries) {
                if (entry.getName().equals("esv.xml")) {
                    esvXmlEntry = entry;
                } else {
                    binaryEntry = entry;
                }
            }
            assertNotNull(esvXmlEntry);
            assertNotNull(binaryEntry);
            InputStream binaryInput = zipFile.getInputStream(binaryEntry);
            assertEquals("test", IOUtils.toString(binaryInput));
            binaryInput.close();

            if (log.isDebugEnabled()) {
                log.debug("Created package at " + file.getPath());
            }
            ContentResourceLoader contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
            esvIn = contentResourceLoader.getResourceAsStream("esv.xml");
            session.importEnhancedSystemViewXML("/test", esvIn,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW,
                    contentResourceLoader);
            assertTrue(session.nodeExists("/test/test"));
            final Node test = session.getNode("/test/test");
            assertTrue(test.hasProperty("test"));
            binaryInput = test.getProperty("test").getBinary().getStream();
            assertEquals("test", IOUtils.toString(binaryInput));
            binaryInput.close();
        } finally {
            IOUtils.closeQuietly(esvIn);
            zipFile.close();
            if (!log.isDebugEnabled()) {
                FileUtils.deleteQuietly(file);
            }
        }
    }

    @Test
    public void testImportFromFileURLWithResources() throws Exception {
        HippoSession session = (HippoSession) this.session;

        File tempFile = File.createTempFile("test-import-fuwr", ".tmp");
        File tempDir = new File(tempFile.getParentFile(), StringUtils.removeEnd(tempFile.getName(), ".tmp"));
        tempDir.mkdir();

        InputStream xmlInput = null;
        InputStream binaryInput;

        try {
            Map<String, File> entryFilesMap = unzipFileTo(session.exportEnhancedSystemViewPackage("/test", true), tempDir);
            xmlInput = new FileInputStream(entryFilesMap.get("esv.xml"));
            ContentResourceLoader contentResourceLoader = new FileContentResourceLoader(tempDir);
            session.importEnhancedSystemViewXML("/test", xmlInput,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW,
                    contentResourceLoader);
            assertTrue(session.nodeExists("/test/test"));
            final Node test = session.getNode("/test/test");
            assertTrue(test.hasProperty("test"));
            binaryInput = test.getProperty("test").getBinary().getStream();
            assertEquals("test", IOUtils.toString(binaryInput));
            binaryInput.close();
        } finally {
            IOUtils.closeQuietly(xmlInput);
            FileUtils.deleteQuietly(tempFile);
            FileUtils.cleanDirectory(tempDir);
        }
    }

    private Map<String, File> unzipFileTo(File file, File targetBaseDir) throws IOException {
        Map<String, File> entryFilesMap = new HashMap<String, File>();
        ZipFile zipFile = new ZipFile(file);
        try {
            for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                InputStream in = null;
                OutputStream out = null;
                File targetFile = null;
                try {
                    in = zipFile.getInputStream(zipEntry);
                    targetFile = new File(targetBaseDir, zipEntry.getName());
                    out = new FileOutputStream(targetFile);
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
                entryFilesMap.put(zipEntry.getName(), targetFile);
            }
        } finally {
            zipFile.close();
        }
        return entryFilesMap;
    }
}
