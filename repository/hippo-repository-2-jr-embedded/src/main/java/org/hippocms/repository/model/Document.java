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

public class Document {
    private String name;
    private String content;
    private DocumentTemplate docTemplate;
    private Workflow workflow;
    private String creator;

    public Document() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DocumentTemplate getDocumentTemplate() {
        return docTemplate;
    }

    public void setDocumentTemplate(DocumentTemplate docTemplate) {
        this.docTemplate = docTemplate;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String username) {
        creator = username;
    }

    public String getModifier() {
        return null;
    }
}
