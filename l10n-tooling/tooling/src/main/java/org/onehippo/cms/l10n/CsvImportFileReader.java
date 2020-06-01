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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvImportFileReader implements ImportFileReader {

    private String format;

    public CsvImportFileReader(final String format) {
        this.format = format;
    }

    @Override
    public List<String[]> read(final File file) throws IOException {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
             final BufferedReader reader = new BufferedReader(inputStreamReader))
        {
            final CSVParser parser = new CSVParser(reader, CSVFormat.valueOf(format));
            final List<CSVRecord> csvRecords = parser.getRecords();
            final List<String[]> records = new ArrayList<>();
            for (CSVRecord csvRecord : csvRecords) {
                final String[] fields = new String[csvRecord.size()];
                for (int i = 0; i < csvRecord.size(); i++) {
                    fields[i] = csvRecord.get(i);
                }
                records.add(fields);
            }

            return records;
        }
    }

}
