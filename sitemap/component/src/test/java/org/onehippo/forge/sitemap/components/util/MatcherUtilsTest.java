/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.util;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class MatcherUtilsTest {
    @Test
    public void testObtainMatchedNodeForMatcher() throws Exception {
        String pathWithMatcher = "/content/documents/news/${1}";
        String expected = "pressrelease-2012";
        String pathToMatch = "/content/documents/news/" + expected;
        int index = 1;

        String result = MatcherUtils.obtainMatchedNodeForMatcher(pathWithMatcher, pathToMatch, index);
        Assert.assertEquals(result, expected);
    }

    @Test
    public void testReplacePlaceholdersWithMatchedNodes() throws Exception {
        String input = "dada --- ${1}/${2}/${3} dumtidum";
        String expected = "dada --- a/g/l dumtidum";
        String[] matchedNodes = {"a", "g", "l"};
        Assert.assertEquals(expected, MatcherUtils.replacePlaceholdersWithMatchedNodes(input, Arrays.asList(matchedNodes)));
    }

    @Test
    public void testGetMatcherForIndex() throws Exception {
        Assert.assertEquals(MatcherUtils.getMatcherForIndex(1), "${1}");
        Assert.assertEquals(MatcherUtils.getMatcherForIndex(10), "${10}");
        Assert.assertEquals(MatcherUtils.getMatcherForIndex(11), "${11}");
        Assert.assertEquals(MatcherUtils.getMatcherForIndex(-1), "${-1}");
    }

    @Test
    public void testExtractPlaceholderValues() throws Exception {
        Map<Integer, String> result = MatcherUtils.extractPlaceholderValues("${3}://${1}/lfaskdjflkas/${2}", "a://b/lfaskdjflkas/c");
        Assert.assertEquals(result.get(1), "b");
        Assert.assertEquals(result.get(2), "c");
        Assert.assertEquals(result.get(3), "a");
    }

    @Test
    public void testGetCommaSeparatedValues() throws Exception {

        final String input = "  value1, \n\t value2  ,value3 , value4,value5";
        final String[] expected = new String[]{"value1","value2","value3","value4","value5"};
        final String[] actual = MatcherUtils.getCommaSeparatedValues(input);

        Assert.assertEquals("Lengths of resulting arrays don't match", 5, actual.length);
        Assert.assertEquals(expected[0], actual[0]);
        Assert.assertEquals(expected[1], actual[1]);
        Assert.assertEquals(expected[2], actual[2]);
        Assert.assertEquals(expected[3], actual[3]);
        Assert.assertEquals(expected[4], actual[4]);

    }

    @Test
    public void testParsePropertyValueOK() throws Exception {

        final String input1 = "property=value";
        final String input2 = "property = value";
        final String input3 = "property \t\n = \t\n value";
        final String[] expectedA = new String[]{"property","value"};

        assertPropertyValue(input1, expectedA);
        assertPropertyValue(input2, expectedA);
        assertPropertyValue(input3, expectedA);

        final String input4 = "namespace:property=value";
        final String input5 = "namespace:property = value";
        final String input6 = "namespace:property \t\n = \t\n value";
        final String[] expectedB = new String[]{"namespace:property","value"};

        assertPropertyValue(input4, expectedB);
        assertPropertyValue(input5, expectedB);
        assertPropertyValue(input6, expectedB);
    }

    private void assertPropertyValue(final String input, final String[] expected) {

        final String[] actual = MatcherUtils.parsePropertyValue(input);

        Assert.assertEquals(2, actual.length);
        Assert.assertEquals(expected[0], actual[0]);
        Assert.assertEquals(expected[1], actual[1]);
    }
}
