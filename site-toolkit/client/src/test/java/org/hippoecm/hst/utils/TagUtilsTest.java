/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.getQueryString;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;
import static org.junit.Assert.assertThat;

public class TagUtilsTest {

    @Test
    public void testToJSONMap_with_null_map() {
        assertThat(toJSONMap(null), is("{}"));
    }

    @Test
    public void testToJSONMap_with_empty_map() {
        assertThat(toJSONMap(Collections.emptyMap()), is("{}"));
    }

    @Test
    public void testToJSONMap_with_non_null_map() {
        final Map<String, Object> map = new HashMap<>();
        map.put("a", "#");
        assertThat(toJSONMap(map), is("{\"a\":\"#\"}"));
    }

    @Test
    public void testToHTMLComment_with_null_value() {
        assertThat(encloseInHTMLComment(null), is("<!--  -->"));
    }

    @Test
    public void testToHTMLComment_with_whitespace_value() {
        assertThat(encloseInHTMLComment("      "), is("<!--  -->"));
    }

    @Test
    public void testToHTMLComment() {
        final Map<String, Object> map = new HashMap<>();
        map.put("a", "#");
        assertThat(encloseInHTMLComment(toJSONMap(map)), is("<!-- {\"a\":\"#\"} -->"));
    }

    @Test
    public void testGetQueryString_with_null_values() throws UnsupportedEncodingException {
        final Map<String, List<String>> params = ImmutableMap.<String, List<String>>builder()
                .put("A", asList(null, "X", null, "Y"))
                .put("B", new ArrayList<>())
                .put("C", asList("P", null, "Q", null, null))
                .build();
        final List<String> removed = asList("D", "E");

        final String result = getQueryString("UTF-8", params, removed);
        assertThat(result, is("?A=X&A=Y&C=P&C=Q"));
    }

    @Test
    public void testGetQueryString_with_empty_params() throws UnsupportedEncodingException {
        final Map<String, List<String>> params = Collections.emptyMap();
        final List<String> removed = null;

        final String result = getQueryString("UTF-8", params, removed);
        assertThat(result, is(""));
    }


    @Test
    public void testGetQueryString_with_all_params_removed() throws UnsupportedEncodingException {
        final Map<String, List<String>> params = ImmutableMap.<String, List<String>>builder()
                .put("A", asList("X", "Y"))
                .put("B", new ArrayList<>())
                .put("C", asList("P", null, "Q"))
                .build();
        final List<String> removed = asList("A", "B", "C", "D", "E");

        final String result = getQueryString("UTF-8", params, removed);
        assertThat(result, is(""));
    }

    @Test
    public void testGetQueryString_with_empty_param_values() throws UnsupportedEncodingException {
        final Map<String, List<String>> params = new HashMap<>();
        params.put("A", null);
        params.put("B", new ArrayList<>());
        params.put("C", asList("X"));
        final List<String> removed = Collections.emptyList();

        final String result = getQueryString("UTF-8", params, removed);
        assertThat(result, is("?C=X"));
    }

    @Test
    public void testGetQueryString_encodes_values() throws UnsupportedEncodingException {
        final Map<String, List<String>> params = ImmutableMap.<String, List<String>>builder()
                .put("key-0", asList("<", ">"))
                .put("key-1", asList(":", "&"))
                .build();
        final List<String> removed = Collections.emptyList();

        final String result = getQueryString("UTF-8", params, removed);
        assertThat(result, is("?key-0=%3C&key-0=%3E&key-1=%3A&key-1=%26"));
    }
}
