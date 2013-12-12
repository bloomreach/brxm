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

/**
 * This is a base interface for all beans that represent a hippo resource should implement.
 */

public interface HippoResourceBean extends HippoBean {

    /**
     * Get the mime type of this resource.
     *
     * @return the mime type of this resource
     */
    String getMimeType();

    /**
     *
     * @return the number of bytes of binary stored in this resource
     */
     long getLength();

     /**
      * Return size in kilobytes
      *
      * @return kilobytes
      */
     BigDecimal getLengthKB();

     /**
      * Return size in kilobytes
      *
      * @return megabytes
      */
     BigDecimal getLengthMB();

     /**
      * @return the last modified date of this resource and <code>null</code> if there is no last modified (should never happen though)
      */
     Calendar getLastModified();

    /**
     * Return <code>true</code> if this resource contains blank data and should not be used by the frontend.
     * This differs from <code>getLength() == 0</code> which may represent an empty file.
     *
     * @return true if this resource contains blank data
     */
    boolean isBlank();
}
