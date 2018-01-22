/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 */

package org.hippoecm.repository.util;

import javax.jcr.Session;

import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.Test;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class UserUtilsTest {

    @Test
    public void getUserNameNoFirstName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(" Doe ");
        replay(session, workspace, securityService, user);

        assertEquals(UserUtils.getUserName("admin", session).get(),"Doe");

        verify(session, workspace, securityService, user);
    }

    @Test
    public void getUserNameNoLastName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(null);
        replay(session, workspace, securityService, user);

        assertEquals(UserUtils.getUserName("admin", session).get(),"John");

        verify(session, workspace, securityService, user);
    }

    @Test
    public void getUserNNameNoLastName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(null);
        replay(session, workspace, securityService, user);

        assertEquals(UserUtils.getUserName("admin", session).get(),"admin");

        verify(session, workspace, securityService, user);
    }

}
