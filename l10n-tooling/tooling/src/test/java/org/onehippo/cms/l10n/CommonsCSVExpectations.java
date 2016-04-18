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
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class CommonsCSVExpectations {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testEscaping() throws Exception {
        final File file = temporaryFolder.newFile("testNewline");
        final String testString = "foo \n bar \r baz \t , comma";
        final CSVFormat csvFormat = CSVFormat.DEFAULT;
        try (final FileWriter writer = new FileWriter(file)) {
            final CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            printer.printRecord(testString);
            printer.flush();
        }
        try (final FileReader reader = new FileReader(file)) {
            final CSVParser parser = new CSVParser(reader, csvFormat);
            final CSVRecord record = parser.getRecords().get(0);

            assertEquals(testString, record.get(0));
        }
    }

}
