/*
 * Copyright 2021 Bloomreach (http://www.bloomreach.com)
 */

package org.onehippo.cms.channelmanager.content.document;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public class DocumentValidityServiceIT extends RepositoryTestCase {

    private DocumentValidityServiceImpl documentValidityService;
    private DocumentWorkflow workflow;
    private Node documentHandle;
    private Node workflowSessionHandle;

    @Before
    public void setup() throws Exception {
        documentValidityService = new DocumentValidityServiceImpl();

        // make a backup of the test document which will be modified
        JcrUtils.copy(session, "/docvalidation/documents/test/doc", "/bak");
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.getNode("/docvalidation/documents/test/doc").remove();
        JcrUtils.copy(session, "/bak", "/docvalidation/documents/test/doc");
        session.getNode("/bak").remove();
        session.save();

        super.tearDown();
    }

    @Before
    public void beforeEach() throws RepositoryException {
        documentHandle = session.getNode("/docvalidation/documents/test/doc");

        final String uuid = documentHandle.getIdentifier();
        workflow = getDocumentWorkflow(getHandle(uuid, session));

        final Session internalWorkflowSession = workflow.getWorkflowContext().getInternalWorkflowSession();
        workflowSessionHandle = getHandle(uuid, internalWorkflowSession);
    }

}
