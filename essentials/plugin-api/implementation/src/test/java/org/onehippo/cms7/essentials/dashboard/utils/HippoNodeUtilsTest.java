/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.List;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @version "$Id: HippoNodeUtilsTest.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
@Ignore
public class HippoNodeUtilsTest {

    @Test
    public void testGetProjectNamespaces() throws Exception {
        final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        final Session session = repository.login("admin", "admin".toCharArray());
        final List<String> projectNamespaces = HippoNodeUtils.getProjectNamespaces(session);
        final Set<String> reserved = EssentialConst.HIPPO_BUILT_IN_NAMESPACES;
        for (String r : reserved) {
            assertFalse(projectNamespaces.contains(r));
        }

        session.logout();

    }

    @Test
    public void testGetPrefixFromType() throws Exception {
        assertEquals("prefix", HippoNodeUtils.getPrefixFromType("prefix:name"));
        assertEquals("", HippoNodeUtils.getPrefixFromType(":name"));
        assertEquals(null, HippoNodeUtils.getPrefixFromType("name"));
    }
}
