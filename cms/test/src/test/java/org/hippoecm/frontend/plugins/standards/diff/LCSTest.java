/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.diff;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;

import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.junit.Test;

public class LCSTest {

    void check(String a, String b, String lcs) {
        LinkedList<String> result = new LinkedList<String>();
        for (String str : stringToArray(lcs)) {
            result.add(str);
        }
        assertEquals(result, LCS.getLongestCommonSubsequence(stringToArray(a), stringToArray(b)));
    }

    private String[] stringToArray(String a) {
        String[] result = new String[a.length()];
        for (int i = 0; i < a.length(); i++) {
            result[i] = new String(new char[] { a.charAt(i) });
        }
        return result;
    }

    @Test
    public void testLongestCommonSubsequence() {
        check("abc", "b", "b");
        check("a", "b", "");
        check("abac", "cbac", "bac");
        check("abcabc", "cbacba", "aba");
        check("GTCGTTCGGAATGCCGTTGCTCTGTAAA", "ACCGGTCGAGTGCGCGGAAGCCGGCCGAA", "GTCGTCGGAAGCCGGCCGAA");
    }

}
