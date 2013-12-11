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
package org.onehippo.cms7.repository.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentUpgrade79Test {

    @Test
    public void testHardDocumentOnFolderIsReplacedByReferenceable() throws IOException {
        File file = File.createTempFile("test", "xml");
        FileUtils.copyURLToFile(getClass().getResource("/input.xml"), file);

        ContentUpgrade79 upgrade = new ContentUpgrade79();
        upgrade.process(file);

        final long crc = FileUtils.checksumCRC32(file);
        File tmpOut = File.createTempFile("verify", "xml");
        FileUtils.copyURLToFile(getClass().getResource("/output.xml"), tmpOut);
        final long verify = FileUtils.checksumCRC32(tmpOut);

        if (verify != crc) {
            IOUtils.copy(new FileInputStream(file), System.out);
        }

        assertEquals("Checksums of expected output and provided output do not match", verify, crc);
    }
}
