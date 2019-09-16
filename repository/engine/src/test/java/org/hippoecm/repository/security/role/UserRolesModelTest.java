/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.role;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.impl.RepositoryDecorator;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.testutils.RepositoryTestCase;

import com.google.common.collect.ImmutableSet;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_USERROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_USERROLEFOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_ADMIN;

public class UserRolesModelTest extends RepositoryTestCase {

    private static String TEST_USERROLES_PATH = "/test/"+HIPPO_USERROLES;

    private InternalHippoRepository internalHippoRepository;
    private UserRolesModel model;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        internalHippoRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(session.getRepository());
    }

    @Override
    public void tearDown() throws Exception {
        if (model != null) {
            model.close();
            model = null;
        }
        super.tearDown();
    }

    private void removeTestUserRoles() throws RepositoryException {
        if (session.nodeExists("/test/"+HIPPO_USERROLES)) {
            session.removeItem("/test/"+HIPPO_USERROLES);
            session.save();
        }
    }

    private void setupTestUserRoles() throws RepositoryException {
        final Node root = session.getRootNode();
        if (!root.hasNode("test")) {
            root.addNode("test");
        }
        final Node test = root.getNode("test");
        if (test.hasNode(HIPPO_USERROLES)) {
            test.getNode(HIPPO_USERROLES).remove();
        }
        final Node userRoles = test.addNode(HIPPO_USERROLES, NT_USERROLEFOLDER);
        Node userRole = userRoles.addNode(USERROLE_ADMIN, NT_USERROLE);
        userRole.setProperty(HIPPO_SYSTEM, true);
        userRole = userRoles.addNode("foo", NT_USERROLE);
        userRole.setProperty(HIPPO_USERROLES, new String[]{"bar"});
        userRole = userRoles.addNode("bar", NT_USERROLE);
        userRole.setProperty(HIPPO_USERROLES, new String[]{USERROLE_ADMIN});
        session.save();
    }

    @Test
    public void testModelSyncOnJcrEvents() throws Exception {
        removeTestUserRoles();
        Session systemSession = internalHippoRepository.createSystemSession();
        model = new UserRolesModel(systemSession, TEST_USERROLES_PATH);
        systemSession.logout();
        assertNull(model.getRole(USERROLE_ADMIN));
        setupTestUserRoles();
        Thread.sleep((100));
        assertNotNull(model.getRole(USERROLE_ADMIN));
        assertEquals(ImmutableSet.of(USERROLE_ADMIN, "foo", "bar"), model.resolveRoleNames("foo"));
        session.getWorkspace().move(TEST_USERROLES_PATH+"/"+"bar", TEST_USERROLES_PATH+"/"+"bar2");
        session.getWorkspace().move(TEST_USERROLES_PATH+"/"+"bar2", "/test/"+"bar3");
        session.getWorkspace().move("/test/"+"bar3", TEST_USERROLES_PATH+"/"+"bar3");
        Thread.sleep(100);
        assertEquals(ImmutableSet.of("foo"), model.resolveRoleNames("foo"));
        session.getNode(TEST_USERROLES_PATH+"/"+"foo").setProperty(HIPPO_USERROLES, new String[]{"bar3"});
        session.save();
        Thread.sleep(100);
        session.move(TEST_USERROLES_PATH+"/"+"bar3", TEST_USERROLES_PATH+"/"+"bar2");
        session.move(TEST_USERROLES_PATH+"/"+"bar2", "/test/"+"bar3");
        session.move("/test/"+"bar3", TEST_USERROLES_PATH+"/"+"bar");
        session.save();
        Thread.sleep(100);
        assertEquals(ImmutableSet.of("foo"), model.resolveRoleNames("foo"));
        session.getNode(TEST_USERROLES_PATH+"/"+"foo").setProperty(HIPPO_USERROLES, new String[]{"bar"});
        session.save();
        Thread.sleep(100);
        assertEquals(ImmutableSet.of(USERROLE_ADMIN, "foo", "bar"), model.resolveRoleNames("foo"));
    }
}
