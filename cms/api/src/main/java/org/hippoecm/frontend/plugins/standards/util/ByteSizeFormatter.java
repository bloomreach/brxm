/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.util.io.IClusterable;

/**
 * Formats numeric byte values into human-readable strings.
 */
public class ByteSizeFormatter implements IClusterable {
    private static final long serialVersionUID = 1L;


    private static final double ONE_KB = new Double(1024);
    private static final double ONE_MB = Math.pow(ONE_KB, 2);
    private static final double ONE_GB = Math.pow(ONE_KB, 3);

    private static final String DEFAULT_BYTES_SUFFIX = " bytes";
    private static final String DEFAULT_KB_SUFFIX = " KB";
    private static final String DEFAULT_MB_SUFFIX = " MB";
    private static final String DEFAULT_GB_SUFFIX = " GB";
    private static final int DEFAULT_DECIMAL_PLACES = 2;

    private final String gigabyteSuffix;
    private final String megabyteSuffix;
    private final String kilobyteSuffix;
    private final String byteSuffix;
    private final int decimalPlaces;

    public ByteSizeFormatter() {
        this(DEFAULT_DECIMAL_PLACES);
    }

    public ByteSizeFormatter(int decimalPlaces) {
        this(DEFAULT_GB_SUFFIX, DEFAULT_MB_SUFFIX, DEFAULT_KB_SUFFIX, DEFAULT_BYTES_SUFFIX, decimalPlaces);
    }

    public ByteSizeFormatter(String gigabyteSuffix, String megabyteSuffix, String kilobyteSuffix, String byteSuffix,
            int decimalPlaces) {
        this.gigabyteSuffix = gigabyteSuffix;
        this.megabyteSuffix = megabyteSuffix;
        this.kilobyteSuffix = kilobyteSuffix;
        this.byteSuffix = byteSuffix;
        this.decimalPlaces = decimalPlaces;
    }

    /** Formats filesize in bytes as appropriate to bytes, KB, MB or GB
     *
     * @param filesize in bytes
     * @return formatted filesize
     **/
    public String format(long filesize) {
        if (Math.abs(filesize) < ONE_KB) {
            return filesize + byteSuffix;
        }

        Locale loc = Session.get().getLocale();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(loc != null ? loc : Locale.getDefault());
        numberFormat.setMaximumFractionDigits(decimalPlaces);

        if (Math.abs(filesize) < ONE_MB) {
            return new StringBuilder(numberFormat.format(filesize / ONE_KB)).append(kilobyteSuffix).toString();
        } else if (Math.abs(filesize) < ONE_GB) {
            return new StringBuilder(numberFormat.format(filesize / ONE_MB)).append(megabyteSuffix).toString();
        } else {
            return new StringBuilder(numberFormat.format(filesize / ONE_GB)).append(gigabyteSuffix).toString();
        }
    }

}
