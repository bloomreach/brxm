/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.security.impl;

import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.User;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MEMBERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.CONFIG_GROUPS_PATH;

/**
 * TestJcrAuthenticationProvider
 * @version $Id$
 */
public class TestJcrAuthenticationProvider extends RepositoryTestCase {
    
    private AuthenticationProvider authenticationProvider;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        session.getNode(CONFIG_GROUPS_PATH+"/admin").setProperty(HIPPO_MEMBERS, new String[]{"admin"});
        session.save();
        Repository systemRepo = server.getRepository();
        Credentials systemCreds = new SimpleCredentials("admin", "admin".toCharArray());
        Repository userRepo = server.getRepository();
        
        authenticationProvider = new JcrAuthenticationProvider(systemRepo, systemCreds, userRepo);
    }

    @Override
    @After
    public void tearDown() throws Exception {

        session.getNode(CONFIG_GROUPS_PATH+"/admin").setProperty(HIPPO_MEMBERS, new String[0]);
        session.save();

        super.tearDown();
    }

    @Test
    public void testAuthentication() {
        User user = null;
        
        try {
            user = authenticationProvider.authenticate("admin", "admin".toCharArray());
            assertEquals("admin", user.getName());
        } catch (SecurityException e) {
            fail("Failed to log on by admin: " + e);
        }
        
        Set<Role> roleSet = authenticationProvider.getRolesByUsername(user.getName());
        assertTrue(roleSet.contains(new TransientRole("everybody")));
        assertTrue(roleSet.contains(new TransientRole("admin")));
        assertFalse(roleSet.contains(new TransientRole("editor")));
    }

}
