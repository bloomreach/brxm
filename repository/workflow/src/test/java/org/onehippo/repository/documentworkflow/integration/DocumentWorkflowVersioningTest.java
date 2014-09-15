/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.integration;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DocumentWorkflowVersioningTest extends AbstractDocumentWorkflowIntegrationTest {

    private static final int NO_VERSIONS = 3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        document.setProperty("counter", 0l);
        session.save();
    }

    @Test
    public void testHints() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();

        final Map<String, Serializable> hints = workflow.hints();
        assertNotNull(hints.get("version"));
        assertTrue((Boolean) hints.get("version"));

        assertNotNull(hints.get("listVersions"));
        assertTrue((Boolean) hints.get("listVersions"));

        assertNotNull(hints.get("restoreVersion"));
        assertTrue((Boolean) hints.get("restoreVersion"));

        assertNotNull(hints.get("versionRestoreTo"));
        assertTrue((Boolean) hints.get("versionRestoreTo"));

        assertNotNull(hints.get("retrieveVersion"));
        assertTrue((Boolean) hints.get("retrieveVersion"));
    }

    @Test
    public void testVersionDocumentAndRetrieve() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();
        for (int i = 0; i < NO_VERSIONS; i++) {
            edit();
            workflow.version();
        }
        final Map<Calendar, Set<String>> history = workflow.listVersions();
        assertEquals(NO_VERSIONS, history.size());
        long counter = 0;
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            final Document version = workflow.retrieveVersion(entry.getKey());
            assertEquals(++counter, version.getNode(session).getProperty("counter").getLong());
        }
    }

    @Test
    public void testVersionDocumentAndRestore() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();
        for (int i = 0; i < NO_VERSIONS; i++) {
            edit();
            workflow.version();
        }
        final Map<Calendar, Set<String>> history = workflow.listVersions();
        final Calendar first = history.keySet().iterator().next();
        workflow.restoreVersion(first);

        assertEquals(1l, document.getProperty("counter").getLong());
    }

    private Long edit() throws Exception {
        JcrUtils.ensureIsCheckedOut(document);
        long counter = document.getProperty("counter").getLong();
        document.setProperty("counter", ++counter);
        session.save();
        return counter;
    }

    private DocumentWorkflow getDocumentWorkflow() throws RepositoryException {
        return (DocumentWorkflow) getWorkflow(handle, "default");
    }

    private Workflow getWorkflow(Node node, String category) throws RepositoryException {
        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        return workflowManager.getWorkflow(category, node);
    }

}
