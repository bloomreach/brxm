/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.brokenlinks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

/**
 * GlobCompilerTest
 */
public class GlobCompilerTest {

    private static final String [] EXCLUDE_PATTERNS = {
        "http://www.example.com/vpn/*",
        "${*"
    };

    private Pattern [] excludePatterns;

    @Before
    public void before() throws Exception {
        excludePatterns = new Pattern[EXCLUDE_PATTERNS.length];

        for (int i = 0; i < EXCLUDE_PATTERNS.length; i++) {
            excludePatterns[i] = GlobCompiler.compileGlobPattern(EXCLUDE_PATTERNS[i], Pattern.CASE_INSENSITIVE);
        }
    }

    @Test
    public void testGlobs() throws Exception {
        assertFalse(isExcludedURL("http://www.example.com/"));
        assertFalse(isExcludedURL("http://www.example.com/index.html"));
        assertFalse(isExcludedURL("http://www.example.com/news/"));
        assertFalse(isExcludedURL("http://www.example.com/news/news1.html"));
        assertFalse(isExcludedURL("http://www.example.com/vpn"));
        assertTrue(isExcludedURL("http://www.example.com/vpn/"));
        assertTrue(isExcludedURL("http://www.eXampLe.COM/vPn/CaseInsensitive.html"));
        assertTrue(isExcludedURL("http://www.example.com/vpn/a/b/c"));
        assertTrue(isExcludedURL("${company.vpn.url}/a/b/c"));
    }

    private boolean isExcludedURL(String url) {
        for (Pattern excludePattern : excludePatterns) {
            Matcher m = excludePattern.matcher(url);
            if (m.matches()) {
                return true;
            }
        }

        return false;
    }
}
