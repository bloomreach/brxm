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
package org.hippoecm.hst.util;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestQueryStringBuilder {

    @Test
    public void lower_ascii_characters_in_name_remain_decoded_for_readability() throws UnsupportedEncodingException {
        final QueryStringBuilder builder = new QueryStringBuilder("UTF-8");
        builder.append("foo:bar", "foo:bar"); // : is used in component rendering urls
        assertEquals("?foo:bar=foo%3Abar", builder.toString());
    }

    @Test
    public void lower_ascii_characters_relevant_in_query_are_encoded() throws UnsupportedEncodingException {
        final QueryStringBuilder builder = new QueryStringBuilder("UTF-8");
        builder.append("foo&", "foo&");
        builder.append("bar=", "bar=");
        builder.append("/?:@azAZ09-._~!$'()*+,;", "keep");

        assertEquals("?foo%26=foo%26&bar%3D=bar%3D&/?:@azAZ09-._~!$'()*+,;=keep", builder.toString());
    }

    @Test
    public void non_ascii_characters_are_encoded() throws UnsupportedEncodingException {
        final QueryStringBuilder builder = new QueryStringBuilder("UTF-8");
        builder.append("fœ-bār", "fœ-bār");
        assertEquals("?f%C5%93-b%C4%81r=f%C5%93-b%C4%81r", builder.toString());
    }

}
