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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
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
        final Map<String,Object> map = new HashMap<>();
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
        final Map<String,Object> map = new HashMap<>();
        map.put("a", "#");
        assertThat(encloseInHTMLComment(toJSONMap(map)), is("<!-- {\"a\":\"#\"} -->"));
    }
}
