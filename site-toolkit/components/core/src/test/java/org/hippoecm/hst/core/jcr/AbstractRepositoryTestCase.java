/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;


public class AbstractRepositoryTestCase extends RepositoryTestCase {

    protected static final String PREVIEW_USER_ID = "previewUser";
    protected static final String PREVIEW_USER_PASS = "previewPass";
    protected static final String LIVE_USER_ID = "liveUser";
    protected static final String LIVE_USER_PASS = "livePass";
    protected static final String LIVE_USER_PASSKEY = "jvm://";
    protected static final String TEST_GROUP_ID = "testgroup";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ModifiableRequestContextProvider.set(new MockHstRequestContext());
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);
        // create test user
        createUserAndGroup(users, groups);
        session.save();

    }

    @Override
    @After
    public void tearDown() throws  Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);
        cleanupUserAndGroup(users, groups);
        session.save();
        ModifiableRequestContextProvider.clear();
        super.tearDown();
    }

    private void createUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        Node previewUser = users.addNode(PREVIEW_USER_ID, HippoNodeType.NT_USER);
        previewUser.setProperty(HippoNodeType.HIPPO_PASSWORD, PREVIEW_USER_PASS);
        Node liveUser = users.addNode(LIVE_USER_ID, HippoNodeType.NT_USER);
        liveUser.setProperty(HippoNodeType.HIPPO_PASSWORD, LIVE_USER_PASS);
        liveUser.setProperty(HippoNodeType.HIPPO_PASSKEY, LIVE_USER_PASSKEY);

        // create test group with member test
        Node testGroup = groups.addNode(TEST_GROUP_ID, HippoNodeType.NT_GROUP);
        testGroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[] { PREVIEW_USER_ID, LIVE_USER_ID });
    }

    private void cleanupUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        if (users.hasNode(PREVIEW_USER_ID)) {
            users.getNode(PREVIEW_USER_ID).remove();
        }
        if (users.hasNode(LIVE_USER_ID)) {
            users.getNode(LIVE_USER_ID).remove();
        }
        if (groups.hasNode(TEST_GROUP_ID)) {
            groups.getNode(TEST_GROUP_ID).remove();
        }
    }


}
