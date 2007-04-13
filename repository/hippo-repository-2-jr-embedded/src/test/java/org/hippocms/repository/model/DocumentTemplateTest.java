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
package org.hippocms.repository.model;

import junit.framework.TestCase;
import org.hippocms.repository.workflows.mock.MockWorkflowFactory;

public class DocumentTemplateTest extends TestCase {
    public DocumentTemplateTest() {
        super();
    }

    public DocumentTemplateTest(String name) {
        super(name);
    }

    public void testCreateDoesNotReturnNull() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        docTemplate.setWorkflowFactory(createMockWorkflowFactory());
        Document document = docTemplate.create("Lorem ipsum");

        assertNotNull(document);
    }

    public void testCreatedDocumentHasSpecifiedName() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        String documentTitle = "Lorem ipsum";
        docTemplate.setWorkflowFactory(createMockWorkflowFactory());
        Document document = docTemplate.create(documentTitle);

        assertEquals("Document must have name passed to 'create(...)'", documentTitle, document.getName());
    }

    public void testCreatedDocumentHasDefaultContent() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        String defaultContent = "Foo bar baz qux quux.";
        docTemplate.setDefaultContent(defaultContent);
        docTemplate.setWorkflowFactory(createMockWorkflowFactory());
        Document document = docTemplate.create("Lorem ipsum");

        assertEquals("Document must have default content set for document template", defaultContent, document
                .getContent());
    }

    public void testCreatedDocumentHasCreatingDocumentTemplate() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        docTemplate.setWorkflowFactory(createMockWorkflowFactory());
        Document document = docTemplate.create("Lorem ipsum");

        assertSame("Document must have document template that created it", docTemplate, document.getDocumentTemplate());
    }

    public void testCreatedDocumentHasWorkflow() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        docTemplate.setWorkflowFactory(createMockWorkflowFactory());
        Document document = docTemplate.create("Lorem ipsum");

        assertNotNull("Document must have a workflow", document.getWorkflow());
    }

    private WorkflowFactory createMockWorkflowFactory() {
        return new MockWorkflowFactory();
    }
}
