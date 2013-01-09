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

import org.hippoecm.frontend.plugins.standards.diff.TextDiffer;
import org.junit.Test;

public class TextDifferTest {

    @Test
    public void testDiffer() {
        TextDiffer differ = new TextDiffer();
        assertEquals("aap <span class=\"hippo-diff-added\">noot</span>", differ.diffText("aap", "aap noot"));
        assertEquals("<span class=\"hippo-diff-added\">aap</span> noot", differ.diffText("noot", "aap noot"));
        assertEquals("<span class=\"hippo-diff-removed\">aap</span> noot", differ.diffText("aap noot", "noot"));
        assertEquals("aap <span class=\"hippo-diff-removed\">noot</span>", differ.diffText("aap noot", "aap"));
        assertEquals("<span class=\"hippo-diff-removed\">aap</span> <span class=\"hippo-diff-added\">noot</span>", differ.diffText(
                "aap", "noot"));
        assertEquals("n&lt;ot", differ.diffText("n<ot", "n<ot"));
    }
}
