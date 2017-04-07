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
package org.onehippo.cms7.services.processor.html.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilTest {

    @Test
    public void testConvertCrlfToLf() throws Exception {
        assertEquals("", StringUtil.convertCrlfToLf(""));
        assertEquals(null, StringUtil.convertCrlfToLf(null));
        assertEquals("a\nb", StringUtil.convertCrlfToLf("a\r\nb"));
        assertEquals("a\nb\n", StringUtil.convertCrlfToLf("a\r\nb\r\n"));
        assertEquals("a\n\nb", StringUtil.convertCrlfToLf("a\r\n\r\nb"));
    }

    @Test
    public void testConvertLfToCrlf() throws Exception {
        assertEquals("", StringUtil.convertLfToCrlf(""));
        assertEquals(null, StringUtil.convertLfToCrlf(null));
        assertEquals("a\r\nb", StringUtil.convertLfToCrlf("a\nb"));
        assertEquals("a\r\nb\r\n", StringUtil.convertLfToCrlf("a\nb\n"));
        assertEquals("a\r\n\r\nb", StringUtil.convertLfToCrlf("a\n\nb"));
    }

    @Test
    public void testStringEmpty() throws Exception {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertTrue(StringUtil.isEmpty("  "));
        assertTrue(StringUtil.isEmpty("\n"));
        assertTrue(StringUtil.isEmpty(" \r\n "));

        assertFalse(StringUtil.isEmpty(" test "));
    }
}
