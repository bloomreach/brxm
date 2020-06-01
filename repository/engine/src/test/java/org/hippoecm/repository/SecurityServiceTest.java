/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.After;
import org.junit.Test;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SecurityServiceTest extends RepositoryTestCase {

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final String encodedName = NodeNameCodec.encode("'t hart", true);
        if (users.hasNode(encodedName)) {
            users.getNode(encodedName).remove();
        }
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        if (groups.hasNode(encodedName)) {
            groups.getNode(encodedName).remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testGetUser() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        assertEquals(session.getUserID(), securityService.getUser(session.getUserID()).getId());
        assertEquals(session.getUserID(), ((HippoSession) session).getUser().getId());
    }

    @Test
    public void testGetMemberships() throws Exception {
        final Iterable<Group> memberships = ((HippoSession) session).getUser().getMemberships();
        final Set<String> groupIds = new HashSet<String>();
        for (Group group : memberships) {
            groupIds.add(group.getId());
        }
        assertTrue(groupIds.contains("everybody"));
        assertTrue(groupIds.contains("admin"));
    }

    @Test
    public void testGetGroup() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        assertTrue(securityService.hasGroup("admin"));
        assertNotNull(securityService.getGroup("admin"));
    }

    @Test
    public void testGetMembers() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        final Group group = securityService.getGroup("admin");
        final Iterable<User> members = group.getMembers();
        final Set<String> userIds = new HashSet<String>();
        for (User member : members) {
            userIds.add(member.getId());
        }
        assertTrue(userIds.contains("admin"));
    }

    @Test
    public void testListUsers() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        final Iterator<User> users = securityService.getUsers(-1, -1).iterator();
        assertTrue(users.hasNext());
        assertEquals("admin", users.next().getId());
    }

    @Test
    public void testListGroups() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        final Iterator<Group> groups = securityService.getGroups(-1, -1).iterator();
        assertTrue(groups.hasNext());
        assertEquals("admin", groups.next().getId());
    }

    @Test
    public void testUserSpecialChars() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final Node user = users.addNode(NodeNameCodec.encode("'t hart", true), HippoNodeType.NT_USER);
        session.save();
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        assertTrue(securityService.hasUser(user.getName()));
        assertTrue(securityService.getUser(user.getName()).getId().equals(user.getName()));
    }

    @Test
    public void testGroupSpecialChars() throws Exception {
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node group = groups.addNode(NodeNameCodec.encode("'t hart", true), HippoNodeType.NT_GROUP);
        session.save();
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        assertTrue(securityService.hasGroup(group.getName()));
        assertTrue(securityService.getGroup(group.getName()).getId().equals(group.getName()));
    }
}
