/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id$"
 */
public class VersionUtilsTest {


    @Test
    public void testCompareVersionNumbers() throws Exception {
        int result = VersionUtils.compareVersionNumbers("1.02.01", "1.02.00");
        assertEquals(1, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.00");
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.01");
        assertEquals(-1, result);
        //############################################
        // SNAPSHOT
        //############################################
        result = VersionUtils.compareVersionNumbers("1.02.01-SNAPSHOT", "1.02.00");
        assertEquals(1, result);
        final boolean higherOrSame = VersionUtils.isHigherOrSame("1.02.01-SNAPSHOT", "1.02.00");
        assertTrue(higherOrSame);
        result = VersionUtils.compareVersionNumbers("1.02.00-SNAPSHOT", "1.02.00-SNAPSHOT");
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.02.00", "1.02.01-SNAPSHOT");
        assertEquals(-1, result);
        result = VersionUtils.compareVersionNumbers(null, null);
        assertEquals(0, result);
        result = VersionUtils.compareVersionNumbers("1.00.00", null);
        assertEquals(1, result);
        result = VersionUtils.compareVersionNumbers(null, "1.00.00");
        assertEquals(-1, result);

    }
}
