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
package org.hippoecm.hst.content.beans.standard;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean mapping class for the 'hippo:resource' document type
 */
@Node(jcrType = "hippo:resource")
public class HippoResource extends HippoItem implements HippoResourceBean {

    private static Logger log = LoggerFactory.getLogger(HippoResource.class);

    // 8 * 1024 = 8192
    private static final BigDecimal DIVISOR_K_BYTE = new BigDecimal(8192);
    // 8 * 1024 * 1024 = 8388608
    private static final BigDecimal DIVISOR_M_BYTE = new BigDecimal(8388608);

    public static final String MIME_TYPE_HIPPO_BLANK = "application/vnd.hippo.blank";

    public String getMimeType() {
        return getProperty("jcr:mimeType");
    }

    public String getFilename() {
        return getProperty(HippoNodeType.HIPPO_FILENAME);
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


    public long getLength() {
        if (this.getNode() == null) {
            log.info("Cannot get length for detached node");
            return 0;
        }
        // a hippo:resource has a mandatory jcr:data property by cnd definition so not testing needed
        try {
            return this.getNode().getProperty("jcr:data").getLength();
        } catch (RepositoryException e) {
            log.warn("Error while fetching binary data length.", e);
        }
        return 0;
    }

    @Override
    public Calendar getLastModified() {
        try {
            return this.getNode().getProperty("jcr:lastModified").getDate();
        } catch (RepositoryException e) {
            log.error("Error during fetching mandatory property jcr:lastModified from '{}'. Return null", getValueProvider().getPath());
            return null;
        }
    }

    /**
     * If a resource contains MIME type application/vnd.hippo.blank it is marked as blank and contains no usable data.
     */
    @Override
    public boolean isBlank() {
        return getProperty("jcr:mimeType").equals(MIME_TYPE_HIPPO_BLANK);
    }

}
