/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TestHippoAuthenticationProvider
 * @version $Id$
 */
public class TestHippoAuthenticationProvider extends RepositoryTestCase {
    
    private AuthenticationProvider authenticationProvider;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Repository systemRepo = server.getRepository();
        Credentials systemCreds = new SimpleCredentials("admin", "admin".toCharArray());
        Repository userRepo = server.getRepository();
        
        authenticationProvider = new HippoAuthenticationProvider(systemRepo, systemCreds, userRepo);
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
        assertTrue(roleSet.contains(new TransientRole("admin")));
    }

}
