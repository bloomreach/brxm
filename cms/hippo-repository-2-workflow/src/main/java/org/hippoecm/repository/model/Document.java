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

import java.util.Iterator;

public class Document {
    private String name;
    private String content;
    private DocumentTemplate docTemplate;
    private Workflow workflow;
    private CurrentUsernameSource currentUsernameSource;
    private String creator;
    private String modifier;
    private boolean isPublished;

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

    public void setInitialContent(String content) {
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
        modifier = currentUsernameSource.getCurrentUsername();
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

    public void setCurrentUsernameSource(CurrentUsernameSource currentUsernameSource) {
        this.currentUsernameSource = currentUsernameSource;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String username) {
        creator = username;
    }

    public String getModifier() {
        return modifier;
    }

    public void publish() {
        Iterator publicationSpsIterator = docTemplate.publicationServiceProvidersIterator();
        while (publicationSpsIterator.hasNext()) {
            PublicationServiceProvider publicationSp = (PublicationServiceProvider) publicationSpsIterator.next();
            publicationSp.publish(name, content);
        }
        isPublished = true;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void unpublish() {
        Iterator publicationSpsIterator = docTemplate.publicationServiceProvidersIterator();
        while (publicationSpsIterator.hasNext()) {
            PublicationServiceProvider publicationSp = (PublicationServiceProvider) publicationSpsIterator.next();
            publicationSp.remove(name);
        }
        isPublished = false;
    }
}
