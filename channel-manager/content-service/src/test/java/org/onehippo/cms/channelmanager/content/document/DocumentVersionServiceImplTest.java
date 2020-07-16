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
import java.util.function.BiFunction;

import javax.jcr.RepositoryException;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

@RunWith(EasyMockRunner.class)
public class DocumentVersionServiceImplTest {

    // System Under Test
    private DocumentVersionService sut;

    @Mock
    private BiFunction<UserContext, String, DocumentWorkflow> workflowGetter;
    @Mock
    private DocumentWorkflow documentWorkflow;
    @Mock
    private Document document;

    private SortedMap<Calendar, Set<String>> versions;
    private MockNode mockFrozenNode;
    private UserContext userContext;

    @Before
    public void setUp() throws RepositoryException, WorkflowException, RemoteException {

        versions = new TreeMap<>();
        mockFrozenNode = MockNode.root().addNode("test", HippoNodeType.NT_DOCUMENT);
        userContext = new UserContext(null, null, null);

        expect(workflowGetter.apply(anyObject(), anyString())).andStubAnswer(() -> documentWorkflow);
        replay(workflowGetter);

        expect(documentWorkflow.listVersions()).andStubAnswer(() -> versions);
        expect(documentWorkflow.retrieveVersion(anyObject())).andStubAnswer(() -> document);
        replay(documentWorkflow);

        expect(document.getNode(anyObject())).andStubAnswer(() -> mockFrozenNode);
        replay(document);

        sut = new DocumentVersionServiceImpl(workflowGetter);
    }

    @Test
    public void no_versions() {

        final List<Version> versions = sut.getVersionInfo(null, null, null).getVersions();

        assertThat(versions.isEmpty(), is(true));
    }

    @Test
    public void master_versions() throws RepositoryException {

        final Calendar timestamp = Calendar.getInstance();
        this.versions.put(timestamp, Collections.emptySet());
        final String userName = "user name";
        mockFrozenNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, userName);

        final List<Version> versions = sut.getVersionInfo(null, MASTER_BRANCH_ID, userContext).getVersions();

        assertThat(versions.size(), is(1));
        assertThat(versions.get(0).getTimestamp(), is(timestamp));
        assertThat(versions.get(0).getUserName(), is(userName));
    }

    @Test
    public void branch_versions_with_current_version() throws RepositoryException {

        final Calendar timestamp = Calendar.getInstance();
        this.versions.put(timestamp, Collections.emptySet());
        final String userName = "user name";
        mockFrozenNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, userName);
        final String branchId = "branch id";
        mockFrozenNode.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, branchId);

        final DocumentVersionInfo versionInfo = sut.getVersionInfo(mockFrozenNode.getIdentifier(), branchId, userContext);
        final List<Version> versions = versionInfo.getVersions();

        assertThat(versionInfo.getCurrentVersion(), is(mockFrozenNode.getIdentifier()));
        assertThat(versions.size(), is(1));
        assertThat(versions.get(0).getTimestamp(), is(timestamp));
        assertThat(versions.get(0).getUserName(), is(userName));
    }
}
