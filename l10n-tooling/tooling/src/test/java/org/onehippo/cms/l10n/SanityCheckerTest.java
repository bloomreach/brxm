/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SanityCheckerTest {

    @Test
    public void test_extractSubstitutionPatterns() {
        assertArrayEquals(new String[0], SanityChecker.extractSubstitutionPatterns(null));
        assertArrayEquals(new String[0], SanityChecker.extractSubstitutionPatterns("foo bar"));
        assertArrayEquals(new String[] { "{}" }, SanityChecker.extractSubstitutionPatterns("{}"));
        assertArrayEquals(new String[] { "{0}" }, SanityChecker.extractSubstitutionPatterns("{0}"));
        assertArrayEquals(new String[] { "{{}}" }, SanityChecker.extractSubstitutionPatterns("{{}}"));
        assertArrayEquals(new String[] { "{{ foo bar }}" }, SanityChecker.extractSubstitutionPatterns("{{ foo bar }}"));
        assertArrayEquals(new String[] { "${ 0 }", "{foo}" }, SanityChecker.extractSubstitutionPatterns("${ 0 } {foo}"));
    }

    @Test
    public void test_containSameSubstitutionPatterns() {
        assertTrue(SanityChecker.containSameSubstitutionPatterns("foo", "bar"));
        assertTrue(SanityChecker.containSameSubstitutionPatterns("{}", "{}"));
        assertTrue(SanityChecker.containSameSubstitutionPatterns("{foo}", "{foo}"));
        assertTrue(SanityChecker.containSameSubstitutionPatterns("{foo} bar {baz}", "{baz} bar {foo}"));

        assertFalse(SanityChecker.containSameSubstitutionPatterns("{}", ""));
        assertFalse(SanityChecker.containSameSubstitutionPatterns("{{0}}", "{0}"));
        assertFalse(SanityChecker.containSameSubstitutionPatterns("{foo} bar {baz}", "{foo}"));
        assertFalse(SanityChecker.containSameSubstitutionPatterns("{foo}", "{foo} {bar}"));
    }

}
