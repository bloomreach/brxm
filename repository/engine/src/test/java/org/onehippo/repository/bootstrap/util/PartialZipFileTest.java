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
package org.onehippo.repository.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.ZipTestUtil;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link org.onehippo.repository.bootstrap.util.PartialZipFile}.
 */
public class PartialZipFileTest {

    private File testZipFile;

    @Before
    public void setUp() throws Exception {
        testZipFile = FileUtils.toFile(getClass().getResource("/bootstrap/path with spaces/SubZipFileTest.zip"));
    }

    @Test
    public void unknownSubPath() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "noSuchSubPathInZip");
        ZipTestUtil.assertEntries(subZip);
    }

    @Test
    public void emptySubPath() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "empty");
        ZipTestUtil.assertEntries(subZip, "empty/");
    }

    @Test
    public void subPathWithOneEntry() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "one");
        ZipTestUtil.assertEntries(subZip, "one/", "one/baz");
    }

    @Test
    public void subPathWithTwoEntries() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "two");
        ZipTestUtil.assertEntries(subZip, "two/", "two/bar", "two/foo");
    }

    @Test
    public void enumeratePartialWithNextElementOnly() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "two");
        final Enumeration<? extends ZipEntry> entries = subZip.entries();

        assertEquals("two/", entries.nextElement().getName());
        assertEquals("two/bar", entries.nextElement().getName());
        assertEquals("two/foo", entries.nextElement().getName());
        try {
            entries.nextElement();
            fail("An exception should be thrown because there should be no more entries");
        } catch (NoSuchElementException expected) {
            // all OK
        }
    }

    @Test
    public void getIncludedEntry() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "two");
        assertEquals("two/foo", subZip.getEntry("two/foo").getName());
    }

    @Test
    public void getNonIncludedEntry() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "two");
        assertNull(subZip.getEntry("one/baz"));
    }

    @Test
    public void getNonExistingEntry() throws IOException {
        PartialZipFile subZip = new PartialZipFile(testZipFile, "one");
        assertNull(subZip.getEntry("no/such/entry"));
    }

}
