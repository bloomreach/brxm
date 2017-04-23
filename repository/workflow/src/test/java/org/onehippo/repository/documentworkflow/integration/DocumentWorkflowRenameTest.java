/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.integration;

import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DocumentWorkflowRenameTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void testRenameLiveDocumentFails() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        try (Log4jInterceptor ignored =
                     Log4jInterceptor.onError().deny("org.onehippo.repository.scxml.SCXMLWorkflowExecutor").build()) {
            workflow.rename("failure");
            fail("Shouldn't be able to rename live document");
        } catch (WorkflowException expected) {
            // ignore
        }
    }

    @Test
    public void testRenameUnpublishedDocumentSucceeds() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.rename("success");
        assertEquals("success", handle.getName());
    }

}
