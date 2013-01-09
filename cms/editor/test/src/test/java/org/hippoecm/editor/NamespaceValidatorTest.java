/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor;

import org.hippoecm.editor.NamespaceValidator;
import org.junit.Test;

public class NamespaceValidatorTest {

    void checkName(String name) {
        try {
            NamespaceValidator.checkName(name);
        } catch (Exception ex) {
            // this is OK
            return;
        }
        throw new RuntimeException("Validator accepted, but should have failed");
    }

    void checkURI(String name) {
        try {
            NamespaceValidator.checkURI(name);
        } catch (Exception ex) {
            // this is OK
            return;
        }
        throw new RuntimeException("Validator accepted, but should have failed");
    }
    
    @Test
    public void testAcceptableNames() throws Exception {
        NamespaceValidator.checkName("abc");
        NamespaceValidator.checkName("Abc");
    }

    @Test
    public void testRejectableNames() {
        checkName(null);
        checkName("");
        checkName("abc1");
        checkName("x+y");
        checkName("d:f");
        checkName("A_c");
        checkName("d-f");
    }

    @Test
    public void testValidNamespace() throws Exception {
        NamespaceValidator.checkURI("http://example.org/test/1.0");
    }

    @Test
    public void testInvalidNamespace() {
        checkURI("http://example.org/test");
        checkURI("example.org/test/1.0");
        checkURI("test");
        checkURI("");
        checkURI(null);
    }
}
