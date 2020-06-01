/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow.integration;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.HippoStdNodeType.NT_PUBLISHABLESUMMARY;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;

public class DerivedDataIntegrationTest extends AbstractDocumentWorkflowIntegrationTest {

    @Override
    protected void createDocument(final Node test) throws RepositoryException {
        super.createDocument(test);
        document.addMixin(NT_PUBLISHABLESUMMARY);
        document.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, "hippostd:content");
    }

    @Test
    public void createNewDocument() throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();
        Node unpublished = getVariant(UNPUBLISHED);
        final String actual = unpublished.getProperty(HIPPOSTD_STATESUMMARY).getString();
        Assert.assertEquals("new", actual);
    }

    @Test
    public void testStateSummaryOfPublishedVariant_createNewDocumentAndPublish() throws RepositoryException, WorkflowException, RemoteException {
        createNewDocumentAndPublish();
        Node published = getVariant(PUBLISHED);
        Assert.assertEquals("live", published.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    @Test
    public void testStateSummaryOfUnPublishedVariant_createNewDocumentAndPublish() throws RepositoryException, WorkflowException, RemoteException {
        createNewDocumentAndPublish();
        Node unpublished = getVariant(UNPUBLISHED);
        Assert.assertEquals("new", unpublished.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    private void createNewDocumentAndPublish() throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();
        documentWorkflow.publish();
    }

    @Test
    public void testStateSummaryOfPublishedNode_createNewDocumentAndPublishAndMakeChanges() throws RepositoryException, WorkflowException, RemoteException {

        Node published = createNewDocumentAndPublishAndMakeChanges(PUBLISHED);
        session.save();
        Assert.assertEquals("changed", published.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    @Test
    public void testStateSummaryOfUnPublishedNode_createNewDocumentAndPublishAndMakeChanges() throws RepositoryException, WorkflowException, RemoteException {

        Node unpublished = createNewDocumentAndPublishAndMakeChanges(UNPUBLISHED);
        session.save();
        Assert.assertEquals("changed", unpublished.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    private Node createNewDocumentAndPublishAndMakeChanges(String variant) throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();
        documentWorkflow.publish();
        documentWorkflow.obtainEditableInstance();
        getVariant(UNPUBLISHED).setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());

        documentWorkflow.commitEditableInstance();
        return getVariant(variant);
    }


    @Test
    public void testStateSummaryOfPublishedVariant_createNewDocumentAndPublishBranch() throws RepositoryException, WorkflowException, RemoteException {

        Node published = createNewDocumentAndPublishBranch(PUBLISHED);
        session.save();
        Assert.assertEquals("live", published.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    @Test
    public void testStateSummaryOfUnpublishedVariant_createNewDocumentAndPublishBranch() throws RepositoryException, WorkflowException, RemoteException {
        Node unpublished = createNewDocumentAndPublishBranch(UNPUBLISHED);
        session.save();
        Assert.assertEquals("new", unpublished.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    private Node createNewDocumentAndPublishBranch(String variant) throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();
        documentWorkflow.branch("A", "A");
        documentWorkflow.obtainEditableInstance("master");
        documentWorkflow.commitEditableInstance();
        documentWorkflow.publishBranch("A");
        return getVariant(variant);
    }

    @Test
    public void testStateSummaryOfPublishedVariant_createNewDocumentAndPublishMaster() throws RepositoryException, WorkflowException, RemoteException {
        Node published = createNewDocumentAndPublishMaster(PUBLISHED);
        session.save();
        Assert.assertEquals("live", published.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    @Test
    public void testStateSummaryOfUnpublishedVariant_createNewDocumentAndPublishMaster() throws RepositoryException, WorkflowException, RemoteException {
        Node unpublished = createNewDocumentAndPublishMaster(UNPUBLISHED);
        session.save();
        Assert.assertEquals("new", unpublished.getProperty(HIPPOSTD_STATESUMMARY).getString());
    }

    private Node createNewDocumentAndPublishMaster(String variant) throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();
        documentWorkflow.branch("A", "A");
        documentWorkflow.obtainEditableInstance("master");
        documentWorkflow.commitEditableInstance();
        documentWorkflow.publish();
        return getVariant(variant);
    }


}
