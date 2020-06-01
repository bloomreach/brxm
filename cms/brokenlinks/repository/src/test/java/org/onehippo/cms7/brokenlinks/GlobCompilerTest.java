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

    @Before
    public void before() throws Exception {
    }

    @Test
    public void testSimpleGlobExpressions() throws Exception {
        String [] exprs = {
            "http*://www.example.com/vpn/*", // typical http(s) urls
            "${*"                            // some custom url pattern starting with a variable expression (${...})
        };

        Pattern [] patterns = createPatterns(new GlobCompiler(), exprs, Pattern.CASE_INSENSITIVE);

        assertFalse(matchWithAnyPattern("http://www.example.com/", patterns));
        assertFalse(matchWithAnyPattern("http://www.example.com/index.html", patterns));
        assertFalse(matchWithAnyPattern("http://www.example.com/news/", patterns));
        assertFalse(matchWithAnyPattern("http://www.example.com/news/news1.html", patterns));
        assertFalse(matchWithAnyPattern("http://www.example.com/vpn", patterns));
        assertTrue(matchWithAnyPattern("http://www.example.com/vpn/", patterns));
        assertTrue(matchWithAnyPattern("http://www.eXampLe.COM/vPn/CaseInsensitive.html", patterns));
        assertTrue(matchWithAnyPattern("http://www.example.com/vpn/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("https://www.example.com/vpn/secure/", patterns));
        assertFalse(matchWithAnyPattern("https://www2.example.com/vpn/secure/", patterns));
        assertTrue(matchWithAnyPattern("${company.vpn.url}/a/b/c", patterns));
    }

    @Test
    public void testMatchOneOrZeroUnknownCharGlobExpressions() throws Exception {
        GlobCompiler compiler = new GlobCompiler();
        compiler.setQuestionMatchesZero(true);

        String [] exprs = { "http*://www.?atclinic.com/*" };
        Pattern [] patterns = createPatterns(compiler, exprs, Pattern.CASE_INSENSITIVE);

        assertTrue(matchWithAnyPattern("http://www.batclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("http://www.catclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("https://www.matclinic.com/secure/", patterns));
        assertTrue(matchWithAnyPattern("http://www.atclinic.com/a/b/c", patterns));
        assertFalse(matchWithAnyPattern("http://www.duatclinic.com/a/b/c", patterns));
    }

    @Test
    public void testMatchOneUnknownCharGlobExpressions() throws Exception {
        String [] exprs = { "http*://www.?atclinic.com/*" };
        Pattern [] patterns = createPatterns(new GlobCompiler(), exprs, Pattern.CASE_INSENSITIVE);

        assertTrue(matchWithAnyPattern("http://www.batclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("http://www.catclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("https://www.matclinic.com/secure/", patterns));
        assertFalse(matchWithAnyPattern("http://www.atclinic.com/a/b/c", patterns));
        assertFalse(matchWithAnyPattern("http://www.duatclinic.com/a/b/c", patterns));
    }

    @Test
    public void testCharsetGlobExpressions() throws Exception {
        String [] exprs = { "http*://www.[bc]atclinic.com/*" };
        Pattern [] patterns = createPatterns(new GlobCompiler(), exprs, Pattern.CASE_INSENSITIVE);

        assertTrue(matchWithAnyPattern("http://www.batclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("http://www.catclinic.com/a/b/c", patterns));
        assertTrue(matchWithAnyPattern("https://www.catclinic.com/secure/", patterns));
        assertFalse(matchWithAnyPattern("http://www.example.com/a/b/c", patterns));
    }

    private Pattern [] createPatterns(GlobCompiler compiler, String [] exprs, int options) throws Exception {
        Pattern [] patterns = new Pattern[exprs.length];

        for (int i = 0; i < exprs.length; i++) {
            patterns[i] = compiler.compile(exprs[i], options);
        }

        return patterns;
    }

    private boolean matchWithAnyPattern(String s, final Pattern [] patterns) {
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(s);

            if (m.matches()) {
                return true;
            }
        }

        return false;
    }
}
