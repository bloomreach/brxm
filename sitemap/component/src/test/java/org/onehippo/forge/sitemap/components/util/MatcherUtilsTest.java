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
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.extractPlaceholderValues;
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.replacePlaceholdersWithMatchedNodes;

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
        Assert.assertEquals(expected, replacePlaceholdersWithMatchedNodes(input, Arrays.asList(matchedNodes)));
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
        Map<Integer, String> result = extractPlaceholderValues("${3}://${1}/lfaskdjflkas/${2}", "a://b/lfaskdjflkas/c");
        Assert.assertEquals(result.get(1), "b");
        Assert.assertEquals(result.get(2), "c");
        Assert.assertEquals(result.get(3), "a");
    }
}
