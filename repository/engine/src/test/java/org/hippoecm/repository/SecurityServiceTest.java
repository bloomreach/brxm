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

import java.util.Set;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.Test;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SecurityServiceTest extends RepositoryTestCase {

    @Test
    public void testGetUser() throws Exception {
        final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
        assertEquals(session.getUserID(), securityService.getUser(session.getUserID()).getId());
        assertEquals(session.getUserID(), ((HippoSession) session).getUser().getId());
    }

    @Test
    public void testGetMemberships() throws Exception {
        final Set<String> groupIds = ((HippoSession) session).getUser().getMemberships();
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
        final Set<String> members = group.getMembers();
        assertTrue(members.contains("admin"));
    }
}
