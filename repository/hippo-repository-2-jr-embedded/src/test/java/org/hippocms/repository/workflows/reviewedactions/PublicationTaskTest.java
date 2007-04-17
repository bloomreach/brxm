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
package org.hippocms.repository.workflows.reviewedactions;

import java.util.Date;
import org.hippocms.repository.model.CurrentUsernameSource;
import org.hippocms.repository.model.Document;
import org.hippocms.repository.model.DocumentTemplate;
import org.hippocms.repository.model.PublicationServiceProvider;
import org.hippocms.repository.model.mock.MockScheduler;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class PublicationTaskTest extends MockObjectTestCase {
    private static final long SECONDS_IN_A_DAY = 86400000;

    public PublicationTaskTest() {
        super();
    }

    public PublicationTaskTest(String name) {
        super(name);
    }

    public void testExecutionOfPublicationTaskSendsContentToPublicationSps() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        MockScheduler scheduler = new MockScheduler();
        workflowFactory.setScheduler(scheduler);
        docTemplate.setWorkflowFactory(workflowFactory);

        Mock spMock = mock(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMock.proxy();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        spMock.expects(once()).method("publish").with(eq(name), eq(content));
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        Date publicationDate = new Date(System.currentTimeMillis() + SECONDS_IN_A_DAY);
        workflow.publish(publicationDate, null);

        scheduler.runAllTasks();
    }
}
