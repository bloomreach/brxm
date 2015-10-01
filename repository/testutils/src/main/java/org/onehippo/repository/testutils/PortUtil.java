/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.testutils;

import org.apache.cxf.testutil.common.TestUtil;

public class PortUtil {

    /**
     * Utility method for finding a free TCP port. The mechanism is reasonably thread safe: two subsequent calls will
     * return different port numbers. TODO: The mechanism does not (yet) try to reuse freed up ports. So it may be
     * possible to run out of port numbers.
     *
     * @param cls class used for internal housekeeping; best practise is to pass in the unit test class
     * @return a free port number
     */
    public static int getPortNumber(Class<?> cls) {
        return Integer.parseInt(TestUtil.getPortNumber(cls));
    }
}
