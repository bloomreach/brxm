/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public abstract class ReaderWriterTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    abstract ExportFileWriter getExportFileWriter();

    abstract ImportFileReader getImportFileReader();

    private void test(final String str) throws IOException {
        final File file = temporaryFolder.newFile();

        // create a small table
        final List<String[]> exportData = new ArrayList<>();
        exportData.add(new String[] { str, str, str });
        exportData.add(new String[] { str, str, str });
        exportData.add(new String[] { str, str, str });

        getExportFileWriter().write(file, exportData);

        final List<String[]> actual = getImportFileReader().read(file);

        for (int row = 0; row < 3; row++) {
            for (int cell = 0; cell < 3; cell++) {
                assertEquals("error on row " + row + " cell " + cell, str, actual.get(row)[cell]);
            }
        }
    }

    @Test
    public void testEscaping() throws Exception {
        // quotes in the text, at the beginning and at the end
        test("' foo ' bar '");
        test("\" foo \" bar \"");

        // newlines and other special characters
        test("foo \n bar \r baz \t lux");

        // commas in the text, at the beginning and at the end
        test(", foo , bar ,");

        // non-latin characters
        test("ã ß € 人");
    }

}
