/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.util;

import org.apache.commons.lang.time.DateFormatUtils;

public class SimpleTimeKeeper {

    private long beginTimestamp;
    private long endTimestamp;

    public SimpleTimeKeeper() {
    }

    public void begin() {
        this.beginTimestamp = System.currentTimeMillis();
    }

    public void end() {
        this.endTimestamp = System.currentTimeMillis();

    }

    public String getFormattedBeginTimestamp() {
        return DateFormatUtils.ISO_DATETIME_FORMAT.format(beginTimestamp);
    }

    public String getFormattedEndTimestamp() {
        return DateFormatUtils.ISO_DATETIME_FORMAT.format(endTimestamp);
    }

    public long getDurationMillis() {
        return endTimestamp - beginTimestamp;
    }
}
