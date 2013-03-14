/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestConfigurationUtils {

    private static int[] expectedSchemeNotMatchingResponseCodes = {HttpServletResponse.SC_OK,
            HttpServletResponse.SC_MOVED_PERMANENTLY,
            HttpServletResponse.SC_MOVED_TEMPORARILY, HttpServletResponse.SC_SEE_OTHER, HttpServletResponse.SC_TEMPORARY_REDIRECT,
            HttpServletResponse.SC_FORBIDDEN, HttpServletResponse.SC_NOT_FOUND};

    @Test
    public void testSuppertedSchemeNotMatchingResponseCodes() {
        for (int expectedSchemeNotMatchingResponseCode : expectedSchemeNotMatchingResponseCodes) {
            assertTrue(ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(expectedSchemeNotMatchingResponseCode));
        }
        assertFalse(ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(199));
    }


    @Test
    public void testSuppertedSchemeNotMatchingResponseCodesAsString() {
        assertEquals("200, 301, 302, 303, 307, 403, 404",ConfigurationUtils.suppertedSchemeNotMatchingResponseCodesAsString());
    }
}
