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
package org.hippoecm.repository.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.onehippo.repository.security.StandardPermissionNames;

import com.google.common.collect.Sets;

public class PermissionManagerTest {

    private boolean containsPrivilege(final Privilege[] privileges, final String privilegeName) {
        return Arrays.stream(privileges).anyMatch(s->s.getName().equals(privilegeName));
    }

    @Test
    public void test() {
        PermissionManager permissionManager = PermissionManager.getInstance();

        Privilege p = permissionManager.getOrCreatePrivilege(StandardPermissionNames.JCR_WRITE);
        assertEquals(null, StandardPermissionNames.JCR_WRITE, p.getName());
        assertTrue(p.isAggregate());
        assertEquals(null, 4, p.getDeclaredAggregatePrivileges().length);
        assertEquals(null, 4, p.getAggregatePrivileges().length);
        assertSame(null, permissionManager.getOrCreatePrivilege(Privilege.JCR_WRITE), p);
        p = permissionManager.getOrCreatePrivilege(StandardPermissionNames.JCR_ALL);
        assertTrue(p.isAggregate());
        assertEquals(null, 9, p.getDeclaredAggregatePrivileges().length);
        assertEquals(null, 13, p.getAggregatePrivileges().length);
        assertTrue(containsPrivilege(p.getAggregatePrivileges(), StandardPermissionNames.JCR_WRITE));

        Set<String> privilegeNames = permissionManager.getOrCreatePermissionNames(StandardPermissionNames.JCR_WRITE);
        assertTrue(privilegeNames.equals(StandardPermissionNames.JCR_WRITE_PRIVILEGES));

        assertFalse(containsPrivilege(permissionManager.getCurrentPrivileges(), "test_priv1"));
        p = permissionManager.getOrCreatePrivilege("test_priv1");
        assertEquals(null, "test_priv1", p.getName());
        assertFalse(p.isAggregate());
        assertTrue(containsPrivilege(permissionManager.getCurrentPrivileges(), "test_priv1"));

        assertFalse(containsPrivilege(permissionManager.getCurrentPrivileges(), "test_priv2"));
        assertFalse(containsPrivilege(permissionManager.getCurrentPrivileges(), "test_priv3"));
        privilegeNames = permissionManager.getOrCreatePermissionNames("test_priv1, test_priv2, test_priv3");
        assertEquals(null, 3, privilegeNames.size());
        assertTrue(privilegeNames.contains("test_priv1"));
        assertTrue(privilegeNames.contains("test_priv2"));
        assertTrue(privilegeNames.contains("test_priv3"));
        assertEquals(null, privilegeNames, permissionManager.getOrCreatePermissionNames(privilegeNames));
        assertTrue(containsPrivilege(permissionManager.getCurrentPrivileges(), "test_priv2"));

        privilegeNames = permissionManager.getOrCreatePermissionNames(StandardPermissionNames.JCR_WRITE);
        assertTrue(privilegeNames.equals(StandardPermissionNames.JCR_WRITE_PRIVILEGES));

        privilegeNames = permissionManager.getOrCreatePermissionNames(StandardPermissionNames.JCR_WRITE+", test_priv1");
        assertTrue(privilegeNames.contains("test_priv1"));
        privilegeNames.remove("test_priv1");
        assertTrue(privilegeNames.equals(StandardPermissionNames.JCR_WRITE_PRIVILEGES));

        int privilegesCount = permissionManager.getCurrentPrivileges().length;

        try {
            permissionManager.getOrCreatePrivilege(null);
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege("");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege(" ");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege("a,b");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege(null);
            fail();
        } catch (IllegalArgumentException ignore) {
        }

        assertSame(null, permissionManager.getOrCreatePrivilege("test_priv1"), permissionManager.getOrCreatePrivilege("  test_priv1  "));

        assertTrue(permissionManager.getOrCreatePermissionNames("").isEmpty());
        assertTrue(permissionManager.getOrCreatePermissionNames(" ").isEmpty());
        assertTrue(permissionManager.getOrCreatePermissionNames(" ,").isEmpty());
        assertTrue(permissionManager.getOrCreatePermissionNames(" ,   ,  ").isEmpty());
        permissionManager.getOrCreatePermissionNames(" test_priv1 , test_priv2  , test_priv3 ");
        assertTrue(permissionManager.getOrCreatePermissionNames(Collections.EMPTY_SET).isEmpty());
        assertTrue(permissionManager.getOrCreatePermissionNames(Sets.newHashSet("", "  ", "a,b", null)).isEmpty());

        assertEquals(null, StandardPermissionNames.JCR_ACTIONS, permissionManager.getOrCreatePermissionNames(String.join(",", StandardPermissionNames.JCR_ACTIONS)));
        for (String actionName : StandardPermissionNames.JCR_ACTIONS) {
            assertFalse(containsPrivilege(permissionManager.getCurrentPrivileges(), actionName));
        }
        try {
            permissionManager.getOrCreatePrivilege(Session.ACTION_ADD_NODE);
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege(Session.ACTION_READ);
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege(Session.ACTION_REMOVE);
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            permissionManager.getOrCreatePrivilege(Session.ACTION_SET_PROPERTY);
            fail();
        } catch (IllegalArgumentException ignore) {
        }

        assertFalse(containsPrivilege(permissionManager.getCurrentPrivileges(), StandardPermissionNames.JCR_WRITE));

        assertEquals(null, privilegesCount, permissionManager.getCurrentPrivileges().length);
    }
}
