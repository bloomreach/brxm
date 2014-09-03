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
package org.hippoecm.repository.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.ZipTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link SubZipFile}.
 */
public class SubZipFileTest {

    private File testZipFile;

    @Before
    public void setUp() throws Exception {
        testZipFile = FileUtils.toFile(getClass().getResource("/org/hippoecm/repository/impl/path with spaces/SubZipFileTest.zip"));
    }

    @Test
    public void unknownSubPath() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "noSuchSubPathInZip");
        ZipTestUtil.assertEntries(subZip);
    }

    @Test
    public void emptySubPath() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "empty");
        ZipTestUtil.assertEntries(subZip, "empty/");
    }

    @Test
    public void subPathWithOneEntry() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "one");
        ZipTestUtil.assertEntries(subZip, "one/", "one/baz");
    }

    @Test
    public void subPathWithTwoEntries() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "two");
        ZipTestUtil.assertEntries(subZip, "two/", "two/bar", "two/foo");
    }

    @Test
    public void getIncludedEntry() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "two");
        assertEquals("two/foo", subZip.getEntry("two/foo").getName());
    }

    @Test
    public void getNonIncludedEntry() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "two");
        assertNull(subZip.getEntry("one/baz"));
    }

    @Test
    public void getNonExistingEntry() throws IOException {
        SubZipFile subZip = new SubZipFile(testZipFile, "one");
        assertNull(subZip.getEntry("no/such/entry"));
    }

}
