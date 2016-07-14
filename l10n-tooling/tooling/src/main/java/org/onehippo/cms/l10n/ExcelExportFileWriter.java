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
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;

public class ExcelExportFileWriter implements ExportFileWriter {

    public ExcelExportFileWriter() {
    }

    @Override
    public void write(final File file, final List<String[]> data) throws IOException {
        final Workbook workbook = new XSSFWorkbook();
        final Sheet sheet = workbook.createSheet();
        sheet.setColumnWidth(1, 20000);
        sheet.setColumnWidth(2, 20000);
        final CellStyle defaultStyle = workbook.getCellStyleAt(0);
        defaultStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        final CellStyle textStyle = workbook.createCellStyle();
        textStyle.setWrapText(true);
        textStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);

        for (int r = 0 ; r < data.size(); r++) {
            final Row row = sheet.createRow(r);
            for (int c = 0; c < data.get(r).length; c++) {
                final Cell cell = row.createCell(c, CELL_TYPE_STRING);
                cell.setCellValue(data.get(r)[c]);
                if (c > 0) {
                    cell.setCellStyle(textStyle);
                }
            }
        }

        final FileOutputStream fileOut = new FileOutputStream(file);
        workbook.write(fileOut);
        fileOut.close();
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

}
