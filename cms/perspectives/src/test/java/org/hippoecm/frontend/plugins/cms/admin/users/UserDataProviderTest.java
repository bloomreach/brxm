/*
 * Copyright 2021 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.cms.admin.users;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.session.AccessiblePluginUserSession;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.UserSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALUSER;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertNotNull;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_DEFAULT_USER_SYSTEM_ADMIN;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_REPOSITORY_BROWSER_USER;

public class UserDataProviderTest extends PluginTest {

    private static final String USERS_PATH = "/hippo:configuration/hippo:users";
    private static final String EXTERNAL_ADMIN_ID = "admin2";
    private static final String EXTERNAL_ADMIN_PASS = "admin2";

    private static final String INTERNAL_TEST_ID = "test";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node users = session.getNode(USERS_PATH);
        final Node admin2 = users.addNode(EXTERNAL_ADMIN_ID, NT_EXTERNALUSER);
        admin2.setProperty(HIPPO_PASSWORD, EXTERNAL_ADMIN_PASS);
        admin2.setProperty(HIPPO_USERROLES, new String[]{USERROLE_DEFAULT_USER_SYSTEM_ADMIN, USERROLE_REPOSITORY_BROWSER_USER});
        users.addNode(INTERNAL_TEST_ID, NT_USER);
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode(USERS_PATH);
        users.getNode(EXTERNAL_ADMIN_ID).remove();
        users.getNode(INTERNAL_TEST_ID).remove();
        session.save();
        super.tearDown();
    }

    @Test
    public void test_instantiating_internal_user_as_external_admin() throws LoginException, RepositoryException {
        final UserSession userSession = new AccessiblePluginUserSession(RequestCycle.get().getRequest());
        userSession.login("admin", "admin");
        ThreadContext.setSession(userSession);

        final UserDataProvider userDataProvider = new UserDataProvider();
        User user = userDataProvider.createBean(userSession.getJcrSession().getNode(String.join("/", USERS_PATH, INTERNAL_TEST_ID)));

        assertNotNull("The user is retrieved with internal admin", user);

        userSession.login(EXTERNAL_ADMIN_ID, EXTERNAL_ADMIN_PASS);
        user = userDataProvider.createBean(userSession.getJcrSession().getNode(String.join("/", USERS_PATH, INTERNAL_TEST_ID)));

        assertNotNull("The user is also retrieved with external admin", user);
    }
}
