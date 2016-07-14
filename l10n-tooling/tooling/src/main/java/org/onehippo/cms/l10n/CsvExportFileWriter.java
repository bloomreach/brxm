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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvExportFileWriter implements ExportFileWriter {

    private String format;

    public CsvExportFileWriter(final String format) {
        this.format = format;
    }

    @Override
    public void write(final File file, final List<String[]> data) throws IOException {
        try (final FileOutputStream fos = new FileOutputStream(file);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8))
        {
            final CSVFormat csvFormat = CSVFormat.valueOf(format);
            final CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            for (final String[] row : data) {
                printer.printRecord((Object[]) row);
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

}
