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
package org.hippoecm.frontend.plugins.xinha.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests JsonParser.
 */
public class JsonParserTest {

    private WicketTester tester;
    private Component dummyComponent;

    @Before
    public void setUp() {
        tester = new WicketTester();
        dummyComponent = new JsonParserTestDummyComponent("dummy-id");
        tester.startComponent(dummyComponent);
    }

    @After
    public void tearDown() {
        tester.destroy();
        tester = null;
        dummyComponent = null;
    }

    @Test
    public void AsDictionary() throws Exception {
        List<String> keyList = Arrays.asList(new String[]{"h1", "pre"});
        String dictionary = JsonParser.asDictionary(keyList, false, false, "xinha.formatblock.", dummyComponent);
        assertEquals("{'h1' : 'Heading 1', 'pre' : 'Formatted'}", dictionary);
    }

    @Test
    public void AsDictionaryWithLabel() throws Exception {
        List<String> keyList = Arrays.asList(new String[]{"h1", "pre"});
        String dictionary = JsonParser.asDictionary(keyList, true, false, "xinha.formatblock.", dummyComponent);
        assertEquals("{'' : '— format —', 'h1' : 'Heading 1', 'pre' : 'Formatted'}", dictionary);
    }

    @Test
    public void AsDictionaryReversed() throws Exception {
        List<String> keyList = Arrays.asList(new String[]{"h2", "pre"});
        String dictionary = JsonParser.asDictionary(keyList, true, true, "xinha.formatblock.", dummyComponent);
        assertEquals("{'— format —' : '', 'Heading 2' : 'h2', 'Formatted' : 'pre'}", dictionary);
    }

    @Test
    public void asKeyValueArray() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", "myId");
        map.put("index", "1");
        map.put("checked", "true");
        assertEquals("[{ key : 'id', value : 'myId'}, { key : 'index', value : 1}, { key : 'checked', value : true}]",
                JsonParser.asKeyValueArray(map));
    }

    @Test
    public void asArrayFromStrings() throws Exception {
        List<String> list = Arrays.asList(new String[]{"foo", "bar", "true", "1"});
        assertEquals("  [\n    'foo',\n    'bar',\n    true,\n    1\n  ]", JsonParser.asArray(list));
    }

}
