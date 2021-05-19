/*
 *  Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelImportFileReader implements ImportFileReader {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportFileReader.class);

    public ExcelImportFileReader() {
    }

    @Override
    public List<String[]> read(final File file) throws IOException {

        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(file))) {
            final List<String[]> data = new ArrayList<>();
            final Sheet sheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                final List<String> rowData = new ArrayList<>();
                boolean add = true;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        log.warn("Skipping translation on row " + row.getRowNum() + ": cell " + c + " is empty");
                        add = false;
                        break;
                    }
                    if (cell.getCellType() != CellType.STRING) {
                        log.warn("Skipping translation on row " + row.getRowNum() + ": cell " + c + " does not contain a string");
                        add = false;
                        break;
                    }
                    rowData.add(cell.getStringCellValue());
                }
                if (add) {
                    data.add(rowData.toArray(new String[0]));
                }
            }

            return data;
        } catch (EncryptedDocumentException e) {
            throw new IOException("Could not read file", e);
        }
    }

}
