/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.solr;


import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternTester {


   // *_[^a-z][^A-Z]

    @Test
    public void testCompoundFieldNamePattern() {
        Pattern compoundFieldNamePatternMatcher = Pattern.compile("._[a-zA-Z]");

        String nonMatching = "test";
        String nonMatching2 = "myfield_";
        String matching = "myfield_txt";
        assertFalse(compoundFieldNamePatternMatcher.matcher(nonMatching).find());
        assertFalse(compoundFieldNamePatternMatcher.matcher(nonMatching2).find());
        assertTrue(compoundFieldNamePatternMatcher.matcher(matching).find());


    }
}
