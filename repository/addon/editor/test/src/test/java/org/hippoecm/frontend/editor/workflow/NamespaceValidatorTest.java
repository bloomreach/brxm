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
package org.hippoecm.frontend.editor.workflow;

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
    public void acceptAlphabeticName() throws Exception {
        NamespaceValidator.checkName("abc");
    }

    @Test
    public void rejectNonAlphabeticName() {
        checkName("abc1");
        checkName("x-y");
        checkName("d:f");
    }

    @Test
    public void acceptVersionedNamespace() throws Exception {
        NamespaceValidator.checkURI("http://example.org/test/1.0");
    }

    @Test
    public void rejectNonversionedNamespace() {
        checkURI("http://example.org/test");
    }

    @Test
    public void rejectNonHttpNamespace() {
        checkURI("example.org/test/1.0");
    }
}
