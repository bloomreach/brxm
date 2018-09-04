/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
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
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_RELAXED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;
import static org.onehippo.repository.util.JcrConstants.NT_VERSION;

public class BranchHandleImplIT extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void getPublished() throws WorkflowException, RepositoryException, RemoteException {

        {
            final Node published = new BranchHandleImpl(MASTER_BRANCH_ID, handle).getPublished();
            assertThat(published, is(nullValue()));
        }

        getDocumentWorkflow(handle).publish();
        {
            final BranchHandle branchHandle = new BranchHandleImpl(MASTER_BRANCH_ID, handle);
            final Node published = branchHandle.getPublished();
            assertThat(getParentNodeType(published), is(NT_HANDLE));
            assertThat(branchHandle.isLiveAvailable(), is(true));
            assertThat(published.hasProperty(HIPPO_PROPERTY_BRANCH_ID), is(false));
        }

        final String branchId = "xyz";
        getDocumentWorkflow(handle).branch(branchId, branchId);
        getDocumentWorkflow(handle).publishBranch(branchId);
        {
            final Node published = new BranchHandleImpl(branchId, handle).getPublished();
            assertTrue(published.isNodeType(NT_FROZEN_NODE));
            assertTrue("even frozen nodes should be decorated to HippoNode", published instanceof HippoNode);
            assertThat(getParentNodeType(published), is(NT_VERSION));
            assertThat(published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString(), is(branchId));
        }

        {
            final BranchHandleImpl branchHandle = new BranchHandleImpl("pqr", handle);
            final Node published = branchHandle.getPublished();
            assertThat(published, is(nullValue()));
        }
    }

    @Test
    public void getDraft_and_check_isModified() throws WorkflowException, RepositoryException, RemoteException, InterruptedException {

        getDocumentWorkflow(handle).publish();
        final Document document = getDocumentWorkflow(handle).obtainEditableInstance();
        final Node node = document.getNode(session);
        node.setProperty("title", "Master title");
        session.save();
        TimeUnit.MILLISECONDS.sleep(100);
        getDocumentWorkflow(handle).commitEditableInstance();

        {
            final BranchHandle branchHandle = new BranchHandleImpl(MASTER_BRANCH_ID, handle);
            assertThat(branchHandle.isMaster(), is(true));
            assertThat(branchHandle.isModified(), is(true));

            final Node draft = branchHandle.getDraft();
            assertThat(draft, is(not(nullValue())));
            assertThat(getParentNodeType(draft), is(NT_HANDLE));
        }

        final String branchId = "xyz";
        getDocumentWorkflow(handle).branch(branchId, branchId);
        getDocumentWorkflow(handle).checkoutBranch(branchId);
        {
            final BranchHandle branchHandle = new BranchHandleImpl(branchId, handle);
            assertThat(branchHandle.isModified(), is(true));
        }
    }

    @Test
    public void getUnpublished() throws WorkflowException, RepositoryException {

        {
            final Node unpublished = new BranchHandleImpl(MASTER_BRANCH_ID, handle).getUnpublished();
            assertThat(unpublished, is(not(nullValue())));
            assertThat(getParentNodeType(unpublished), is(NT_HANDLE));
        }

        final String branchId = "xyz";
        getDocumentWorkflow(handle).branch(branchId, branchId);
        getDocumentWorkflow(handle).checkoutBranch(MASTER_BRANCH_ID);
        {
            final Node unpublished = new BranchHandleImpl(branchId, handle).getUnpublished();
            assertThat(getParentNodeType(unpublished), is(NT_VERSION));
        }

    }

    @Test
    public void unpersisted_branch_handle_does_not_log_warning_or_info_related_to_versioning_when_not_yet_persisted() throws Exception {
        final Node test = handle.getParent();
        final Node unpersistedHandle = test.addNode("foo", NT_HANDLE);
        final Node foo = unpersistedHandle.addNode("foo", NT_DOCUMENT);
        foo.addMixin(HIPPOSTDPUBWF_DOCUMENT);
        foo.addMixin(MIX_VERSIONABLE);
        foo.addMixin(NT_RELAXED);
        foo.setProperty(HIPPOSTDPUBWF_CREATION_DATE, Calendar.getInstance());
        foo.setProperty(HIPPOSTDPUBWF_CREATED_BY, "testuser");
        foo.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        foo.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, "testuser");
        foo.setProperty(HIPPOSTD_STATE, UNPUBLISHED);
        foo.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});

        final DocumentHandle documentHandle = new DocumentHandle(unpersistedHandle);
        documentHandle.initialize();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(BranchHandleImpl.class).build()) {

            documentHandle.getBranchHandle().isModified();
            
            Assertions.assertThat(interceptor.messages().filter(s -> s.contains("Cannot get frozen node of document")))
                    .as("Unexpected log message")
                    .isEmpty();


            Assertions.assertThat(interceptor.messages().filter(s -> s.contains("Could not get version history")))
                    .as("Unexpected log message")
                    .isEmpty();
        }

    }


    private String getParentNodeType(final Node published) throws RepositoryException {
        return published.getParent().getPrimaryNodeType().getName();
    }
}