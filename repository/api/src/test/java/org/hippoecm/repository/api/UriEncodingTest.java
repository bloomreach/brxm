/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests StringCodecFactory$UriEncoding
 */
public class UriEncodingTest {

    private StringCodecFactory.UriEncoding uri;

    @Before
    public void setUp() throws Exception {
        uri = new StringCodecFactory.UriEncoding();
    }

    @Test(expected = NullPointerException.class)
    public void encodeNull() {
        uri.encode(null);
    };

    @Test
    public void encode() {
        // simple values
        assertEquals("", uri.encode(""));
        assertEquals("text", uri.encode("text"));
        assertEquals("1234567890", uri.encode("1234567890"));

        // spaces
        assertEquals("one-space", uri.encode("one space"));
        assertEquals("two-spaces", uri.encode("two  spaces"));
        assertEquals("three-spaces", uri.encode("three   spaces"));
        assertEquals("one-leading-space", uri.encode(" one leading space"));
        assertEquals("two-leading-spaces", uri.encode("  two leading spaces"));
        assertEquals("three-leading-spaces", uri.encode("   three leading spaces"));
        assertEquals("one-ending-space", uri.encode("one ending space "));
        assertEquals("two-ending-spaces", uri.encode("two ending spaces  "));
        assertEquals("three-ending-spaces", uri.encode("three ending spaces   "));

        // mixed casing
        assertEquals("no-mixed-casing", uri.encode("NO Mixed Casing"));
        assertEquals("weird-casing", uri.encode("WeIrD CaSiNG"));

        // special characters
        assertEquals("info-at-hippo.com", uri.encode("info@hippo.com"));
        assertEquals("under_score", uri.encode("under_score"));
        assertEquals("a-at-bcusddefg-hij-k-l-mnop-q-r-s-tuvwxy-z", uri.encode("!a@b#c$d%e^f&g*h(i)j-k=l+m[n]o}p\\q|r;s:t'u\"v,w<x>y/z?"));
        assertEquals("-a", uri.encode("~a"));

        // foreign characters
        assertEquals("хиппо-устойчивости", uri.encode("Хиппо устойчивости"));
        assertEquals("hippo-可持续性", uri.encode("Hippo 可持续性"));

        // trailing dots
        assertEquals("abc", uri.encode("abc."));
        assertEquals("a", uri.encode("a..."));
        assertEquals("e...e", uri.encode("e...e"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void decode() {
        uri.decode("test");
    }

}
