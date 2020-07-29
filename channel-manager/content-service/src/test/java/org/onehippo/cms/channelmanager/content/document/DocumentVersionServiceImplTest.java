/*
 * Copyright 2020 Bloomreach
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
package org.onehippo.cms.channelmanager.content.document;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.JcrConstants;
import org.easymock.EasyMockRunner;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;
import org.onehippo.repository.mock.MockVersion;
import org.onehippo.repository.mock.MockVersionManager;

import static org.apache.jackrabbit.JcrConstants.MIX_VERSIONABLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

@RunWith(EasyMockRunner.class)
public class DocumentVersionServiceImplTest {

    // System Under Test
    private DocumentVersionService sut;

    private MockNode mockNode;
    private MockVersionManager versionManager ;
    private UserContext userContext;

    private String userId = "john";
    private Calendar mockNodeLastModified;

    @Before
    public void setUp() throws RepositoryException, WorkflowException, RemoteException {

        mockNodeLastModified = Calendar.getInstance();

        mockNode = MockNode.root().addNode("test", HippoNodeType.NT_DOCUMENT);
        mockNode.addMixin(MIX_VERSIONABLE);
        mockNode.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, mockNodeLastModified);
        mockNode.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, userId);

        final MockSession session = mockNode.getSession();
        session.setUserId(userId);

        versionManager = session.getWorkspace().getVersionManager();


        userContext = new UserContext(session, null, null);

        sut = new DocumentVersionServiceImpl();
    }

    @Test
    public void workspace_only_master() {

        final List<Version> versions = sut.getVersionInfo(mockNode.getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat("Expected workspace version to be present", versions.size(), is(1));

        assertThat(versions.get(0).getTimestamp().getTimeInMillis(), is(mockNodeLastModified.getTimeInMillis()));
        assertThat(versions.get(0).getUserName(), is(userId));
        assertThat(versions.get(0).getJcrUUID(), is(mockNode.getIdentifier()));
        assertThat(versions.get(0).getBranchId(), is(MASTER_BRANCH_ID));
    }

    @Test
    public void non_existing_branch() {

        final List<Version> versions = sut.getVersionInfo(mockNode.getIdentifier(), "non-existing", userContext).getVersions();

        assertThat(versions.size(), is(0));

    }

    @Test
    public void master_with_version_in_history() throws RepositoryException {


        versionManager.checkin(mockNode.getPath());
        versionManager.checkout(mockNode.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockNode.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final List<Version> versions = sut.getVersionInfo(mockNode.getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(versions.size(), is(2));

        assertThat(versions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(versions.get(0).getUserName(), is(userId));
        assertThat(versions.get(0).getJcrUUID(), is(mockNode.getIdentifier()));
        assertThat(versions.get(0).getBranchId(), is(MASTER_BRANCH_ID));

        assertFalse("versioned node expected different creation time" ,
                versions.get(1).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(versions.get(1).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                versions.get(1).getJcrUUID().equals(mockNode.getIdentifier()));
        assertThat(versions.get(1).getBranchId(), is(MASTER_BRANCH_ID));
    }

    @Test
    public void fetch_MASTER_versions_via_frozen_nodeid() throws RepositoryException {

        final MockVersion versioned = versionManager.checkin(mockNode.getPath());
        versionManager.checkout(mockNode.getPath());

        // make the workspace version for 'mybranch', the versioned node is for 'master'

        mockNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, "mybranch");

        // fetch via the versioned node the master versions
        final List<Version> masterVersions = sut.getVersionInfo(versioned.getFrozenNode().getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(masterVersions.size(), is(1));

        assertThat(masterVersions.get(0).getUserName(), is(userId));
        assertThat(masterVersions.get(0).getJcrUUID(), is(versioned.getFrozenNode().getIdentifier()));
        assertThat(masterVersions.get(0).getBranchId(), is(MASTER_BRANCH_ID));

        final List<Version> branchVersions = sut.getVersionInfo(mockNode.getIdentifier(), "mybranch", userContext).getVersions();

        assertThat(branchVersions.size(), is(1));

    }

    @Test
    public void branch_with_version_in_history_and_workspace() throws RepositoryException {

        mockNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, "mybranch");

        versionManager.checkin(mockNode.getPath());
        versionManager.checkout(mockNode.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockNode.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final List<Version> masterVersions = sut.getVersionInfo(mockNode.getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(masterVersions.size(), is(0));

        final List<Version> branchVersions = sut.getVersionInfo(mockNode.getIdentifier(), "mybranch", userContext).getVersions();

        assertThat(branchVersions.size(), is(2));

        assertThat(branchVersions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(branchVersions.get(0).getUserName(), is(userId));
        assertThat(branchVersions.get(0).getJcrUUID(), is(mockNode.getIdentifier()));
        assertThat(branchVersions.get(0).getBranchId(), is("mybranch"));

        assertFalse("versioned node expected different creation time" ,
                branchVersions.get(1).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(branchVersions.get(1).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                branchVersions.get(1).getJcrUUID().equals(mockNode.getIdentifier()));
        assertThat(branchVersions.get(1).getBranchId(), is("mybranch"));
    }

    @Test
    public void branch_with_version_in_history_and_workspace_version_is_for_master() throws Exception {
        mockNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, "mybranch");

        // make sure version node has a different creation time
        Thread.sleep(1);
        versionManager.checkin(mockNode.getPath());
        versionManager.checkout(mockNode.getPath());

        mockNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

        final Calendar newDate = Calendar.getInstance();
        mockNode.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final List<Version> masterVersions = sut.getVersionInfo(mockNode.getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(masterVersions.size(), is(1));

        final List<Version> branchVersions = sut.getVersionInfo(mockNode.getIdentifier(), "mybranch", userContext).getVersions();

        assertThat(branchVersions.size(), is(1));

        assertThat(masterVersions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(masterVersions.get(0).getUserName(), is(userId));
        assertThat(masterVersions.get(0).getJcrUUID(), is(mockNode.getIdentifier()));
        assertThat(masterVersions.get(0).getBranchId(), is(MASTER_BRANCH_ID));

        assertFalse("versioned node expected different creation time" ,
                branchVersions.get(0).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(branchVersions.get(0).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                branchVersions.get(0).getJcrUUID().equals(mockNode.getIdentifier()));
        assertThat(branchVersions.get(0).getBranchId(), is("mybranch"));
    }


    @Test
    public void versions_are_ordered_by_workspace_first_and_then_on_creation_time() throws Exception {
        int i = 0;
        // create 10 versions with different creation date (and reset the creation date for the first one at the end
        // to make sure that we do not have a straight order
        MockVersion firstCheckin = null;
        while(i < 10) {
            i++;
            Thread.sleep(1);
            MockVersion next = versionManager.checkin(mockNode.getPath());
            if (firstCheckin == null) {
                firstCheckin = next;
            }
            versionManager.checkout(mockNode.getPath());
        }

        // reset the first one, overriding a version node created property...works for versioned mock nodes
        firstCheckin.setCreated(Calendar.getInstance());

        final List<Version> masterVersions = sut.getVersionInfo(mockNode.getIdentifier(), MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(masterVersions.size(), is(11));

        assertTrue(masterVersions.get(0).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        Version prev = null;
        // the newest versions must be on top (except potentially the first one since that is the workspace version
        for (Version version : masterVersions.stream().skip(1).collect(Collectors.toList())) {
            if (prev == null) {
                prev = version;
                continue;
            }
            assertTrue(prev.getTimestamp().getTimeInMillis() >= version.getTimestamp().getTimeInMillis());
            prev = version;
        }
    }
}
