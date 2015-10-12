/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.util;


import java.io.UnsupportedEncodingException;

import org.junit.Test;

import static org.hippoecm.hst.core.util.PathEncoder.encode;
import static org.junit.Assert.assertEquals;

public class PathEncoderTest {

    @Test
    public void encoder_keeps_slashes_intact() throws UnsupportedEncodingException {
        assertEquals("//www.onehippo.org/foo", encode("//www.onehippo.org/foo", "utf-8", null));
        assertEquals("//www.onehippo.org/foo/", encode("//www.onehippo.org/foo/", "utf-8", null));
        assertEquals("//www.onehippo.org/foo//", encode("//www.onehippo.org/foo//", "utf-8", null));
        assertEquals("////", encode("////", "utf-8", null));
    }

    @Test
    public void encoder_encodes_colon() throws UnsupportedEncodingException {
        assertEquals("http%3A//www.onehippo.org/foo", encode("http://www.onehippo.org/foo", "utf-8", null));
    }

    @Test
    public void encoder_does_skip_matching_prefix() throws UnsupportedEncodingException {
        final String[] ignorePrefixes = new String[]{"http:"};
        assertEquals("http://www.onehippo.org/foo", encode("http://www.onehippo.org/foo", "utf-8", ignorePrefixes));
    }

    @Test
    public void encoder_does_skip_first_matching_prefix() throws UnsupportedEncodingException {

        final String[] ignorePrefixes = new String[]{"http", "http:"};
        // since first matching prefix is 'http', we expect the ':' to be encoded
        assertEquals("http%3A//www.onehippo.org/foo", encode("http://www.onehippo.org/foo", "utf-8", ignorePrefixes));
    }

    @Test
    public void encoder_keeps_first_dash_intact() throws UnsupportedEncodingException {

        final String[] ignorePrefixes = new String[]{"http:"};
        assertEquals("http://www.one#hippo.org/foo", encode("http://www.one#hippo.org/foo", "utf-8", ignorePrefixes));
        assertEquals("http://www.onehippo.org#foo", encode("http://www.onehippo.org#foo", "utf-8", ignorePrefixes));
        assertEquals("http://www.one#hippo.org%23foo", encode("http://www.one#hippo.org#foo", "utf-8", ignorePrefixes));
    }

    @Test
    public void encoder_keeps_all_question_marks_intact() throws UnsupportedEncodingException {
        final String[] ignorePrefixes = new String[]{"http:"};
        assertEquals("http://www.one?hippo.org/foo", encode("http://www.one?hippo.org/foo", "utf-8", ignorePrefixes));
        assertEquals("http://www.onehippo.org/foo?bar=test", encode("http://www.onehippo.org/foo?bar=test", "utf-8", ignorePrefixes));
        assertEquals("http://www.one?hippo.org/foo?bar=test", encode("http://www.one?hippo.org/foo?bar=test", "utf-8", ignorePrefixes));
    }

    @Test
    public void encoder_keeps_ampersand_after_question_mark_intact() throws UnsupportedEncodingException {
        final String[] ignorePrefixes = new String[]{"http:"};
        assertEquals("http://www.one%26hippo.org/foo", encode("http://www.one&hippo.org/foo", "utf-8", ignorePrefixes));
        assertEquals("http://www.onehippo.org/foo?bar=test&foo=lux", encode("http://www.onehippo.org/foo?bar=test&foo=lux", "utf-8", ignorePrefixes));
    }


}