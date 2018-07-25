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
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.junit.Assert.assertThat;
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
            assertThat(branchHandle.isLive(), is(true));
            assertThat(published.hasProperty(HIPPO_PROPERTY_BRANCH_ID), is(false));
        }

        final String branchId = "xyz";
        getDocumentWorkflow(handle).branch(branchId, branchId);
        getDocumentWorkflow(handle).publishBranch(branchId);
        {
            final Node published = new BranchHandleImpl(branchId, handle).getPublished();
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

    private String getParentNodeType(final Node published) throws RepositoryException {
        return published.getParent().getPrimaryNodeType().getName();
    }
}