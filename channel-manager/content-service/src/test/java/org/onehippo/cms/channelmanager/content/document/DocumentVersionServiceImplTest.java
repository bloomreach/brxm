/*
 * Copyright 2020-2021 Bloomreach
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.easymock.EasyMockRunner;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;
import org.onehippo.repository.mock.MockVersion;
import org.onehippo.repository.mock.MockVersionManager;

import static org.apache.jackrabbit.JcrConstants.MIX_VERSIONABLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

@RunWith(EasyMockRunner.class)
public class DocumentVersionServiceImplTest {

    // System Under Test
    private DocumentVersionService sut;

    private MockNode mockHandle;
    private MockNode mockPreview;
    private MockVersionManager versionManager ;
    private UserContext userContext;

    private String userId = "john";
    private Calendar mockNodeLastModified;
    private Map<String,Boolean> mockHints;

    @Before
    public void setUp() throws RepositoryException, WorkflowException, RemoteException {

        mockNodeLastModified = Calendar.getInstance();

        mockHandle = MockNode.root().addNode("test", HippoNodeType.NT_HANDLE);

        mockPreview = mockHandle.addNode("test", HippoNodeType.NT_DOCUMENT);
        mockPreview.addMixin(MIX_VERSIONABLE);
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, mockNodeLastModified);
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, userId);
        mockPreview.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview"});

        final MockSession session = mockHandle.getSession();
        session.setUserId(userId);

        versionManager = session.getWorkspace().getVersionManager();


        userContext = new UserContext(session, null, null);

        mockHints = new HashMap<>();
        mockHints.put("restoreVersion", true);
        sut = new DocumentVersionServiceImpl((handle, branchId) -> mockHints);
    }

    @Test
    public void workspace_only_master() {

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        assertThat("When there are no versions, restore should be disabled", versionInfo.isRestoreEnabled(), is(false));
        final List<Version> versions = versionInfo.getVersions();

        assertThat("Expected workspace version to be present", versions.size(), is(1));

        assertThat(versions.get(0).getTimestamp().getTimeInMillis(), is(mockNodeLastModified.getTimeInMillis()));
        assertThat(versions.get(0).getUserName(), is(userId));
        assertThat(versions.get(0).getJcrUUID(), is(mockPreview.getIdentifier()));
        assertThat(versions.get(0).getBranchId(), is(MASTER_BRANCH_ID));
    }

    @Test(expected = BadRequestException.class)
    public void non_existing_branch() {
        sut.getVersionInfo(mockPreview.getIdentifier(), "non-existing", userContext, false).getVersions();
    }

    @Test
    public void master_with_version_in_history() throws Exception {

        // make sure version node has a different creation time
        Thread.sleep(1);
        versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        assertThat(versionInfo.isRestoreEnabled(), is(true));
        final List<Version> versions = versionInfo.getVersions();

        assertThat(versions.size(), is(2));

        assertThat(versions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(versions.get(0).getUserName(), is(userId));
        assertThat(versions.get(0).getJcrUUID(), is(mockPreview.getIdentifier()));
        assertThat(versions.get(0).getBranchId(), is(MASTER_BRANCH_ID));

        assertFalse("versioned node expected different creation time" ,
                versions.get(1).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(versions.get(1).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                versions.get(1).getJcrUUID().equals(mockPreview.getIdentifier()));
        assertThat(versions.get(1).getBranchId(), is(MASTER_BRANCH_ID));
    }

    @Test
    public void branch_with_version_in_history_and_workspace() throws Exception {

        mockPreview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "mybranch");

        // make sure version node has a different creation time
        Thread.sleep(1);
        versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        assertThat(versionInfo.isRestoreEnabled(), is(false));
        final List<Version> masterVersions = versionInfo.getVersions();

        assertThat(masterVersions.size(), is(0));

        final DocumentVersionInfo branchVersionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), "mybranch", userContext, false);
        assertThat(branchVersionInfo.isRestoreEnabled(), is(true));
        final List<Version> branchVersions = branchVersionInfo.getVersions();

        assertThat(branchVersions.size(), is(2));

        assertThat(branchVersions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(branchVersions.get(0).getUserName(), is(userId));
        assertThat(branchVersions.get(0).getJcrUUID(), is(mockPreview.getIdentifier()));
        assertThat(branchVersions.get(0).getBranchId(), is("mybranch"));

        assertFalse("versioned node expected different creation time" ,
                branchVersions.get(1).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(branchVersions.get(1).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                branchVersions.get(1).getJcrUUID().equals(mockPreview.getIdentifier()));
        assertThat(branchVersions.get(1).getBranchId(), is("mybranch"));
    }

    @Test
    public void branch_with_version_in_history_and_workspace_version_is_for_master() throws Exception {
        mockPreview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "mybranch");

        // make sure version node has a different creation time
        Thread.sleep(1);
        versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        mockPreview.setProperty(HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

        final Calendar newDate = Calendar.getInstance();
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        // for master there is only the published variant
        assertThat(versionInfo.isRestoreEnabled(), is(false));
        final List<Version> masterVersions = versionInfo.getVersions();

        assertThat(masterVersions.size(), is(1));

        final DocumentVersionInfo myBranchDocumentVersionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), "mybranch", userContext, false);
        final List<Version> branchVersions = myBranchDocumentVersionInfo.getVersions();

        // my branch has a version
        assertThat(myBranchDocumentVersionInfo.isRestoreEnabled(), is(true));
        assertThat(branchVersions.size(), is(1));

        assertThat(masterVersions.get(0).getTimestamp().getTimeInMillis(), is(newDate.getTimeInMillis()));
        assertThat(masterVersions.get(0).getUserName(), is(userId));
        assertThat(masterVersions.get(0).getJcrUUID(), is(mockPreview.getIdentifier()));
        assertThat(masterVersions.get(0).getBranchId(), is(MASTER_BRANCH_ID));

        assertFalse("versioned node expected different creation time" ,
                branchVersions.get(0).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertThat(branchVersions.get(0).getUserName(), is(userId));
        assertFalse("versioned node expected different uuid" ,
                branchVersions.get(0).getJcrUUID().equals(mockPreview.getIdentifier()));
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
            MockVersion next = versionManager.checkin(mockPreview.getPath());
            if (firstCheckin == null) {
                firstCheckin = next;
            }
            versionManager.checkout(mockPreview.getPath());
        }

        // reset the first one, overriding a version node created property...works for versioned mock nodes
        firstCheckin.setCreated(Calendar.getInstance());

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        assertThat(versionInfo.isRestoreEnabled(), is(true));
        final List<Version> masterVersions = versionInfo.getVersions();

        assertThat(masterVersions.size(), is(11));

        assertTrue(masterVersions.get(0).getTimestamp().getTimeInMillis() == mockNodeLastModified.getTimeInMillis());

        assertOrderedByDate(masterVersions, version -> version.getTimestamp());
    }

    private void assertOrderedByDate(final List<Version> masterVersions, final Function<Version, Calendar> getDate) {
        Version prev = null;
        // the newest versions must be on top (except potentially the first one since that is the workspace version
        for (Version version : masterVersions.stream().skip(1).collect(Collectors.toList())) {
            if (prev == null) {
                prev = version;
                continue;
            }
            assertTrue(getDate.apply(prev).getTimeInMillis() >= getDate.apply(version).getTimeInMillis());
            prev = version;
        }
    }

    @Test
    public void versions_filtered_by_having_a_campaign() throws Exception {
        MockVersion version = versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);

        {
            final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, true);
            assertThat(versionInfo.isRestoreEnabled(), is(false));
            final List<Version> versions = versionInfo.getVersions();

            // no campaigns set, but the workspace version is still added
            assertThat(versions.size(), is(1));
        }

        // on purpose, we use different timezone as input: this is to show that the final 'versions' contain UTC time
        final Calendar from = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT")).setDate(2019, 1, 2).build();
        final Calendar to = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT")).setDate(2021, 1, 2).build();

        final Calendar fromCompare = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2019, 1, 2).build();
        final Calendar toCompare = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2021, 1, 2).build();

        // set a campaign for the version
        JcrVersionsMetaUtils.setCampaign(mockHandle, new Campaign(version.getFrozenNode().getIdentifier(), from, to));

        {
            final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, true);
            assertThat(versionInfo.isRestoreEnabled(), is(true));
            final List<Version> versions = versionInfo.getVersions();

            // 1 campaigns set, first version is the workspace version
            assertThat(versions.size(), is(2));
            assertEquals(versions.get(1).getCampaign(), new Campaign(version.getFrozenNode().getIdentifier(), fromCompare, toCompare));

        }
    }

    @Test
    public void campaign_versions_with_label() throws Exception {
        MockVersion version = versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        final Calendar newDate = Calendar.getInstance();
        mockPreview.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, newDate);


        // set a campaign for the version and a label
        JcrVersionsMetaUtils.setVersionLabel(mockHandle, new VersionLabel(version.getFrozenNode().getIdentifier(), "My Label"));

        {
            // only fetch campaign versions
            final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
            final List<Version> versions = versionInfo.getVersions();

            // 1 campaigns set
            assertThat(versions.size(), is(2));
            assertEquals(versions.get(1).getLabel(), "My Label");
        }

        // Now also with a campaign
        Calendar from = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2019, 1, 2).build();
        Calendar to = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2021, 1, 2).build();

        // set a campaign for the version
        JcrVersionsMetaUtils.setCampaign(mockHandle, new Campaign(version.getFrozenNode().getIdentifier(), from, to));

        {
            // only fetch campaign versions
            final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, true);

            final List<Version> versions = versionInfo.getVersions();

            // 1 campaigns set, first item is the workspace item
            assertThat(versions.size(), is(2));
            assertEquals(versions.get(1).getCampaign(), new Campaign(version.getFrozenNode().getIdentifier(), from, to));
            assertEquals(versions.get(1).getLabel(), "My Label");
        }
    }

    @Test
    public void restore_hints() throws RepositoryException {

        // add a version otherwise restore is always disabled
        versionManager.checkin(mockPreview.getPath());

        mockHints.clear();
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(false));

        mockHints.put("restoreVersion", true);
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(true));
        mockHints.put("restoreVersion", false);
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(false));

        mockHints.put("restoreVersionToBranch", true);
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(true));
        mockHints.put("restoreVersionToBranch", false);
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(false));

        mockHints.put("restoreVersion", true);
        mockHints.put("restoreVersionToBranch", true);
        assertThat(sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false).isRestoreEnabled(),
                is(true));
    }

    @Test
    public void maximum_of_100_versions_returned_order_by_date() throws Exception {
        // make sure version node has a different creation time
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1);
            versionManager.checkin(mockPreview.getPath());
            versionManager.checkout(mockPreview.getPath());
        }


        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        final List<Version> versions = versionInfo.getVersions();

        // first version is the 'workspace' version
        assertEquals(101, versions.size());

        assertOrderedByDate(versions, version -> version.getTimestamp());

        final Version version98 = versions.get(99);
        final Version version99 = versions.get(100);

        // add one
        Thread.sleep(1);
        versionManager.checkin(mockPreview.getPath());
        versionManager.checkout(mockPreview.getPath());

        final DocumentVersionInfo versionInfo2 = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, false);
        final List<Version> versions2 = versionInfo2.getVersions();

        // first version is the 'workspace' version
        assertEquals(101, versions2.size());

        assertOrderedByDate(versions2, version -> version.getTimestamp());

        assertFalse("Expected version 99 to have dropped off", versions2.stream().anyMatch(version -> version.getJcrUUID().equals(version99.getJcrUUID())));

        assertEquals("Expected version 98 to have moved to position 100",
                version98.getJcrUUID(), versions2.get(100).getJcrUUID());

    }

    @Test
    public void maximum_of_100_campaign_versions_returned_order_by_START_date() throws Exception {
        // make sure version node has a different creation time
        final Random random = new Random();
        for (int i = 0; i < 200; i++) {
            Thread.sleep(1);

            // add a non-campaign version
            versionManager.checkin(mockPreview.getPath());
            versionManager.checkout(mockPreview.getPath());

            // add campaign version
            MockVersion version = versionManager.checkin(mockPreview.getPath());

            versionManager.checkout(mockPreview.getPath());
            // random from and to date
            final Calendar from = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2019, random.nextInt(12) + 1, random.nextInt(27)).build();
            final Calendar to = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2021, random.nextInt(12) + 1, random.nextInt(27)).build();

            // set a campaign for the version
            JcrVersionsMetaUtils.setCampaign(mockHandle, new Campaign(version.getFrozenNode().getIdentifier(), from, to));
        }

        // only get campaign versions, there are 200 created, but we only expect the first 100 sorted on date
        final DocumentVersionInfo versions = sut.getVersionInfo(mockHandle.getIdentifier(), MASTER_BRANCH_ID, userContext, true);

        // first version is the 'workspace' version
        assertEquals(101, versions.getVersions().size());

        // all 100 items after the first one are campaigns
        assertTrue(versions.getVersions().stream().skip(1).anyMatch(version -> version.getCampaign() != null));

        assertOrderedByDate(versions.getVersions(), version -> version.getCampaign().getFrom());
    }
}
