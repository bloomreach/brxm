/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

public class HtmlValidatorTest {

    @Test
    public void testValidHtml() throws Exception {
        String text = "<html><body>aap noot mies</body></html>";
        HtmlValidator validator = new HtmlValidator();
        Set<String> violations = validator.validateNonEmpty(text);
        assertEquals(0, violations.size());
    }

    @Test
    public void testEmptyHtml() throws Exception {
        String text = "<html><body></body></html>";
        HtmlValidator validator = new HtmlValidator();
        Set<String> violations = validator.validateNonEmpty(text);
        assertEquals(1, violations.size());
    }

    @Test
    public void testImageHtml() throws Exception {
        String text = "<html><body><img src=xxx /></body></html>";
        HtmlValidator validator = new HtmlValidator();
        Set<String> violations = validator.validateNonEmpty(text);
        assertEquals(0, violations.size());
    }
}
