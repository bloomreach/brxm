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
package org.hippoecm.hst.mock.content.beans.standard;

import java.math.BigDecimal;
import java.util.Calendar;

import org.hippoecm.hst.content.beans.standard.HippoResourceBean;

/**
 * @version "$Id$"
 */
public class MockHippoResourceBean extends MockHippoBean implements HippoResourceBean {

    private String mimeType;
    private long length;
    private Calendar cal;
    private boolean blank;

    // 8 * 1024 = 8192
    private static final BigDecimal DIVISOR_K_BYTE = new BigDecimal(8192);
    // 8 * 1024 * 1024 = 8388608
    private static final BigDecimal DIVISOR_M_BYTE = new BigDecimal(8388608);

    public long getLength() {
        return length;
    }

    public void setLength(final long length) {
        this.length = length;
    }

    public BigDecimal getLengthKB() {
        // multiple getLength() by 8 to get size in bits
        return calculate(getLength() * 8 , DIVISOR_K_BYTE);
    }

    public BigDecimal getLengthMB() {
        // multiple getLength() by 8 to get size in bits
        return calculate(getLength() * 8, DIVISOR_M_BYTE);
    }

    /**
     * Calculate size for given divisor
     *
     * @param size    size in bits
     * @param divisor divisor we are dividing with
     * @return 0 or BigDecimal containing size using  default scale
     */
    private BigDecimal calculate(final long size, final BigDecimal divisor) {
        if (size == 0L) {
            return new BigDecimal(0);
        }
        return new BigDecimal(size).divide(divisor);
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Calendar getLastModified() {
        return cal;
    }

    public Calendar setLastModified(Calendar cal) {
        return cal;
    }

    @Override
    public boolean isBlank() {
        return blank;
    }

    public void setBlank(boolean blank) {
        this.blank = blank;
    }
}

