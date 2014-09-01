/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.testutils;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Utility methods for unit testing zip files.
 */
public class ZipTestUtil {

    private ZipTestUtil() {}

    public static void assertEntries(ZipFile zip, String... names) {
        final int size = zip.size();
        assertEquals(names.length, size);

        final Enumeration<? extends ZipEntry> entries = zip.entries();
        for (int i = 0; i < size; i++) {
            assertTrue(entries.hasMoreElements());
            assertEquals(names[i], entries.nextElement().getName());
        }
        assertFalse(entries.hasMoreElements());
    }

}
