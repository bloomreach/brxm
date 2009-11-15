/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.standards.util;

import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.text.NumberFormatter;

import org.apache.wicket.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats numeric byte values into human-readable strings.
 */
public class ByteSizeFormatter implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ByteSizeFormatter.class);

    private static final long ONE_KB = 1024;
    private static final long ONE_MB = ONE_KB * ONE_KB;
    private static final long ONE_GB = ONE_KB * ONE_KB * ONE_KB;

    private String gigabyteSuffix;
    private String megabyteSuffix;
    private String kilobyteSuffix;
    private String byteSuffix;

    private NumberFormatter formatter;

    public ByteSizeFormatter() {
        this(" GB", " MB", " KB", " B", 2);
    }

    public ByteSizeFormatter(String gigabyteSuffix, String megabyteSuffix, String kilobyteSuffix, String byteSuffix,
            int decimalPlaces) {
        this.gigabyteSuffix = gigabyteSuffix;
        this.megabyteSuffix = megabyteSuffix;
        this.kilobyteSuffix = kilobyteSuffix;
        this.byteSuffix = byteSuffix;

        String numberFormat = decimalPlaces > 0 ? "0.0" : "0";
        for (int i = 1; i < decimalPlaces; i++) {
            numberFormat += "0";
        }
        formatter = new NumberFormatter(new DecimalFormat(numberFormat));
    }

    public String format(long byteSize) {
        try {
            if (byteSize > ONE_GB) {
                return formatter.valueToString(new Double(byteSize / ONE_GB)) + gigabyteSuffix;
            } else if (byteSize > ONE_MB) {
                return formatter.valueToString(new Double(byteSize / ONE_MB)) + megabyteSuffix;
            } else if (byteSize > ONE_KB) {
                return formatter.valueToString(new Double(byteSize / ONE_KB)) + kilobyteSuffix;
            }
        } catch (ParseException e) {
            if (log.isErrorEnabled()) {
                log.error("Unable to format byte size " + byteSize, e);
            }
        }
        return byteSize + byteSuffix;
    }

}