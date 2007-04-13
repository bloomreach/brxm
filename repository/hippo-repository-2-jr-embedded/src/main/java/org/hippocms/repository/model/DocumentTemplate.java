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

public class DocumentTemplate {
    private String defaultContent;
    private WorkflowFactory workflowFactory;
    private CurrentUsernameSource currentUsernameSource;

    public DocumentTemplate() {
        super();
    }

    public Document create(String name) {
        Document result = new Document();

        result.setName(name);
        result.setInitialContent(defaultContent);
        result.setDocumentTemplate(this);
        result.setWorkflow(workflowFactory.create(result));
        result.setCurrentUsernameSource(currentUsernameSource);
        result.setCreator(currentUsernameSource.getCurrentUsername());

        return result;
    }

    public void setDefaultContent(String defaultContent) {
        this.defaultContent = defaultContent;
    }

    public void setWorkflowFactory(WorkflowFactory workflowFactory) {
        this.workflowFactory = workflowFactory;
    }

    public void setCurrentUsernameSource(CurrentUsernameSource currentUsernameSource) {
        this.currentUsernameSource = currentUsernameSource;
    }
}
