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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TestDefaultLoginModule
 * @version $Id$
 */
public class TestDefaultLoginModule {

    protected Map<String, ?> sharedState;
    protected Map<String, ?> options;
    private Map<String, String []> userRolesMap = new HashMap<String, String []>();
    
    private LoginModule loginModule;
    private AuthenticationProvider authProvider;

    @Before
    public void setUp() {
        sharedState = new HashMap<String, Object>();
        options = new HashMap<String, Object>();
        
        authProvider = new MockAuthenticationProvider(userRolesMap);

        loginModule = new DefaultLoginModule(authProvider);
    }

    @Test
    public void testLogin() {
        userRolesMap.clear();
        userRolesMap.put("charley", new String [] { "user", "dev" });
        
        boolean loggedIn = false;
        boolean committed = false;
        
        Subject subject = new Subject();
        CallbackHandler callbackHandler = new PassiveCallbackHandler("nobody", "nobody".toCharArray());
        loginModule.initialize(subject, callbackHandler, sharedState, options);
        
        try {
            loggedIn = loginModule.login();
            fail("login should fail!");
        } catch (LoginException e) {
        }
        
        loggedIn = false;
        committed = false;
        
        subject = new Subject();
        callbackHandler = new PassiveCallbackHandler("charley", "brown".toCharArray());
        loginModule.initialize(subject, callbackHandler, sharedState, options);
        
        try {
            loggedIn = loginModule.login();
            assertTrue("Failed to log in.", loggedIn);
            
            committed = loginModule.commit();
            assertTrue("Failed to commit.", committed);
            
            assertEquals("The count of principals of this subject is wrong.", 3, subject.getPrincipals().size());
            assertEquals("The count of user principals of this subject is wrong.", 1, subject.getPrincipals(User.class).size());
            assertEquals("The count of role principals of this subject is wrong.", 2, subject.getPrincipals(Role.class).size());
            
            assertEquals("charley", subject.getPrincipals(User.class).iterator().next().getName());
            assertTrue("user role not found.", subject.getPrincipals(Role.class).contains(new TransientRole("user")));
            assertTrue("dev role not found.", subject.getPrincipals(Role.class).contains(new TransientRole("dev")));
            assertFalse("manager role should not be found.", subject.getPrincipals(Role.class).contains(new TransientRole("manager")));
        } catch (LoginException e) {
            e.printStackTrace();
            fail("login failed.");
        }
    }
    
    @Ignore
    private static class MockAuthenticationProvider implements AuthenticationProvider {
        
        private Map<String, String []> userRolesMap;
        
        public MockAuthenticationProvider(Map<String, String []> userRolesMap) {
            this.userRolesMap = userRolesMap;
        }

        public User authenticate(String userName, char[] password) throws SecurityException {
            for (String user : userRolesMap.keySet()) {
                if (user.equals(userName)) {
                    return new TransientUser(user);
                }
            }
            
            throw new SecurityException("User not found: " + userName);
        }

        public Set<Role> getRolesByUsername(String username) throws SecurityException {
            String [] roles = userRolesMap.get(username);
            
            if (roles == null) {
                throw new SecurityException("User not found: " + username);
            }
            
            Set<Role> roleSet = new HashSet<Role>();
            
            for (String role : roles) {
                roleSet.add(new TransientRole(role));
            }
            
            return roleSet;
        }
    }
}
