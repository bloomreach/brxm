/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.model;

import junit.framework.TestCase;
import org.hippoecm.repository.workflows.mock.MockWorkflowFactory;

public class DocumentTest extends TestCase {
    public DocumentTest() {
        super();
    }

    public DocumentTest(String name) {
        super(name);
    }

    public void testDocumentHasCorrectModifierAfterChange() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(new MockWorkflowFactory());
        Document doc = docTemplate.create("Lorem ipsum");

        String modifierName = "Jane Doe";
        currentUsernameSource.setCurrentUsername(modifierName);
        doc.setContent("Quux qux baz bar foo.");

        assertEquals("Modifier must be set when document is changed", modifierName, doc.getModifier());
    }
}
