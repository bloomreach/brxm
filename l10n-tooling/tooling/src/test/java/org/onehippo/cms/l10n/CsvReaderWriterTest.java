/*
 *  Copyright 2016-2023 Bloomreach
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

public class CsvReaderWriterTest extends ReaderWriterTest {

    @Override
    ExportFileWriter getExportFileWriter() {
        return new CsvExportFileWriter("Default");
    }

    @Override
    ImportFileReader getImportFileReader() {
        return new CsvImportFileReader("Default");
    }

}
