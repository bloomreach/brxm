/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.security.service.SecurityServiceImpl;
import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_EMAIL;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ACTIVE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LASTLOGIN;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LASTSYNC;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MEMBERS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALGROUP;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALUSER;
import static org.hippoecm.repository.api.HippoNodeType.NT_GROUP;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.util.JcrConstants.JCR_PATH;
import static org.onehippo.repository.util.JcrConstants.JCR_PRIMARY_TYPE;
import static org.onehippo.repository.util.JcrConstants.JCR_UUID;

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
    public void testSecurityServiceRequiresSystemSession() throws Exception {
        InternalHippoRepository internalRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(server.getRepository());
        try {
            new SecurityServiceImpl(internalRepository.getHippoSecurityManager(), session);
            fail("creating a SecurityServiceImpl should require a system session");
        } catch (IllegalArgumentException e) {
            assertEquals("systemSession really must be a system session", e.getMessage());
        }
        new SecurityServiceImpl(internalRepository.getHippoSecurityManager(), internalRepository.createSystemSession()).close();
    }

    @Test
    public void testGetUser() throws Exception {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        assertEquals(session.getUserID(), securityService.getUser(session.getUserID()).getId());
        assertEquals(session.getUserID(), ((HippoSession) session).getUser().getId());
    }

    @Test
    public void testGetMemberships() throws Exception {
        final Set<String> memberships = ((HippoSession) session).getUser().getMemberships();
        assertTrue(memberships.contains("everybody"));
        assertTrue(memberships.contains("admin"));
    }

    @Test
    public void testGetGroup() throws Exception {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        assertTrue(securityService.hasGroup("admin"));
        assertNotNull(securityService.getGroup("admin"));
    }

    @Test
    public void testGetMembers() throws Exception {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        final Group group = securityService.getGroup("admin");
        final Set<String> members = group.getMembers();
        assertTrue(members.contains("admin"));
    }

    @Test
    public void testListUsers() throws Exception {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        final Iterator<User> users = securityService.getUsers(-1, -1).iterator();
        assertTrue(users.hasNext());
        assertEquals("admin", users.next().getId());
    }

    @Test
    public void testListGroups() throws Exception {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        final Iterator<Group> groups = securityService.getGroups(-1, -1).iterator();
        assertTrue(groups.hasNext());
        assertEquals("admin", groups.next().getId());
    }

    @Test
    public void testUserSpecialChars() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final Node user = users.addNode(NodeNameCodec.encode("'t hart", true), NT_USER);
        session.save();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        assertTrue(securityService.hasUser(user.getName()));
        assertTrue(securityService.getUser(user.getName()).getId().equals(NodeNameCodec.decode(user.getName())));
    }

    @Test
    public void testGroupSpecialChars() throws Exception {
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node group = groups.addNode(NodeNameCodec.encode("'t hart", true), NT_GROUP);
        session.save();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        assertTrue(securityService.hasGroup(group.getName()));
        assertTrue(securityService.getGroup(group.getName()).getId().equals(NodeNameCodec.decode(group.getName())));
    }

    @Test
    public void testUserAndGroupSpecialChars() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final Node userNode = users.addNode(NodeNameCodec.encode("'t hart", true), NT_USER);
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node groupNode = groups.addNode(NodeNameCodec.encode("'t hart", true), NT_GROUP);
        groupNode.setProperty(HIPPO_MEMBERS, new String[]{"'t hart"});
        session.save();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        assertTrue(securityService.hasUser(userNode.getName()));
        User user = securityService.getUser(userNode.getName());
        assertTrue(securityService.hasGroup(groupNode.getName()));
        Group group = securityService.getGroup(groupNode.getName());
        assertTrue(user.getMemberships().contains(group.getId()));
        assertTrue(group.getMembers().contains(user.getId()));
    }

    @Test
    public void testUserGetProperty() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final Node userNode = users.addNode(NodeNameCodec.encode("'t hart", true), NT_EXTERNALUSER);
        userNode.setProperty(HIPPOSYS_EMAIL, "foo@bar.com");
        userNode.setProperty(HIPPO_SYSTEM, true);
        userNode.setProperty(HIPPO_ACTIVE, false);
        userNode.setProperty("foo", "bar");
        userNode.setProperty(HIPPO_PASSWORD, "random");
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        userNode.setProperty(HIPPO_LASTLOGIN, calendar);
        String calendarString = userNode.setProperty(HIPPO_LASTSYNC, calendar).getString();
        session.save();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        User user = securityService.getUser(userNode.getName());
        assertTrue(user.isSystemUser());
        assertFalse(user.isActive());
        assertNull(user.getProperty(JCR_PRIMARY_TYPE));
        assertNull(user.getProperty(JCR_PATH));
        assertNull(user.getProperty(JCR_UUID));
        assertNull(user.getProperty(HIPPO_PASSWORD));
        assertNull(user.getProperty(HIPPO_SYSTEM));
        assertNull(user.getProperty(HIPPO_ACTIVE));
        assertEquals("bar", user.getProperty("foo"));
        assertEquals("foo@bar.com", user.getEmail());
        assertEquals("foo@bar.com", user.getProperty(HIPPOSYS_EMAIL));
        assertEquals(now, user.getLastLogin().getTime());
        assertNull(user.getProperty(HIPPO_LASTLOGIN));
        assertEquals(calendarString, user.getProperty(HIPPO_LASTSYNC));
    }

    @Test
    public void testGroupGetProperty() throws Exception {
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node groupNode = groups.addNode(NodeNameCodec.encode("'t hart", true), NT_EXTERNALGROUP);
        groupNode.setProperty(HIPPO_SYSTEM, true);
        groupNode.setProperty(HIPPOSYS_DESCRIPTION, "a group");
        groupNode.setProperty("foo", "bar");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String calendarString = groupNode.setProperty("hipposys:syncdate", calendar).getString();
        session.save();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        Group group = securityService.getGroup(groupNode.getName());
        assertTrue(group.isSystemGroup());
        assertNull(group.getProperty(JCR_PRIMARY_TYPE));
        assertNull(group.getProperty(JCR_PATH));
        assertNull(group.getProperty(JCR_UUID));
        assertNull(group.getProperty(HIPPO_SYSTEM));
        assertEquals("bar", group.getProperty("foo"));
        assertEquals("a group", group.getDescription());
        assertEquals("a group", group.getProperty(HIPPOSYS_DESCRIPTION));
        assertEquals(calendarString, group.getProperty("hipposys:syncdate"));
    }
}
