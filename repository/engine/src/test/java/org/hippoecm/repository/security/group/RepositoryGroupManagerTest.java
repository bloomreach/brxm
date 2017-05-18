/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.group;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.junit.After;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import junit.framework.Assert;

/**
 */
public class RepositoryGroupManagerTest extends RepositoryTestCase {

    ManagerContext managerContext;
    private static final String GROUP_NAME = "external-editors";
    private static final String USER_NAME = "external-editor-1";
    private static final String GROUPFOLDER_PREFIX = "groupfolder";
    private static final String GROUP_PREFIX = "group";
    private static final String TESTUSER = "testuser";

    @After
    @Override
    public void tearDown() throws Exception {

        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        if (groups.hasNode(GROUP_NAME)) {
            groups.getNode(GROUP_NAME).remove();
        }
        if (groups.hasNode(GROUPFOLDER_PREFIX + "1")) {
            groups.getNode(GROUPFOLDER_PREFIX + "1").remove();
        }

        session.save();
        super.tearDown();
    }

    /**
     * test REPO-1487
     * @throws Exception
     */
    @Test
    public void testAddMember() throws Exception {
        //first create an empty group
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node group = groups.addNode(NodeNameCodec.encode(GROUP_NAME, true), HippoNodeType.NT_GROUP);
        Assert.assertNotNull(group);
        session.save();

        managerContext = new ManagerContext(session, "hippo:configuration/hippo:security/internal", "hippo:configuration/hippo:groups",true);
        RepositoryGroupManager repositoryGroupManager = new RepositoryGroupManager();
        repositoryGroupManager.init(managerContext);

        repositoryGroupManager.addMember(group, USER_NAME);

        Assert.assertTrue(repositoryGroupManager.getMembers(group).contains(USER_NAME));
    }

    /**
     * When dirlevels = 0 (default) then group memberships on a deeper level may not be read.
     */
    @Test
    public void testGroupReadDirLevels() throws Exception {
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        createGroupFolder(groups, 1);
        session.save();

        managerContext = new ManagerContext(session, "hippo:configuration/hippo:security/internal", "hippo:configuration/hippo:groups", true);
        RepositoryGroupManager repositoryGroupManager = new RepositoryGroupManager();
        repositoryGroupManager.init(managerContext);

        final Set<String> membershipIds = repositoryGroupManager.getMembershipIds(TESTUSER);
        Assert.assertFalse("Membership of a group below the current dirlevel", membershipIds.contains("group1"));

    }

    private void createGroupFolder(final Node parentNode, final int number) throws RepositoryException {
        final Node groupfolder = parentNode.addNode(GROUPFOLDER_PREFIX + number, HippoNodeType.NT_GROUPFOLDER);
        final Node group = groupfolder.addNode(GROUP_PREFIX + number, HippoNodeType.NT_GROUP);
        final Value testuser = session.getValueFactory().createValue(TESTUSER, PropertyType.STRING);
        group.setProperty("hipposys:members", new Value[] { testuser });
    }

}
