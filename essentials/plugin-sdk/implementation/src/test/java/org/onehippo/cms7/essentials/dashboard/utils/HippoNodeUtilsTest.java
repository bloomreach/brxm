/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.List;
import java.util.Set;

import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: HippoNodeUtilsTest.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public class HippoNodeUtilsTest extends BaseRepositoryTest {

    @Test
    public void testGetProjectNamespaces() throws Exception {
        Session session = getSession();
        final List<String> projectNamespaces = HippoNodeUtils.getProjectNamespaces(session);
        final Set<String> reserved = EssentialConst.HIPPO_BUILT_IN_NAMESPACES;
        for (String r : reserved) {
            assertFalse(projectNamespaces.contains(r));
        }
        session.logout();
    }

    @Test
    public void testGetNameFromType() throws Exception {
        assertEquals(null, HippoNodeUtils.getNameFromType(""));
        assertEquals("document", HippoNodeUtils.getNameFromType("hippo:document"));
        assertEquals("document", HippoNodeUtils.getNameFromType("document"));
    }

    @Test
    public void testGetPrefixFromType() throws Exception {
        assertEquals("prefix", HippoNodeUtils.getPrefixFromType("prefix:name"));
        assertEquals("", HippoNodeUtils.getPrefixFromType(":name"));
        assertEquals(null, HippoNodeUtils.getPrefixFromType("name"));
        assertEquals(null, HippoNodeUtils.getPrefixFromType(""));
    }

    @Test
    public void testResolvePath() throws Exception {
        assertEquals("/hippo:namespaces/someprefix/document", HippoNodeUtils.resolvePath("someprefix:document"));
        assertEquals("/hippo:namespaces/system/String", HippoNodeUtils.resolvePath("String"));
    }

    @Test
    public void testGetCompounds() throws Exception {
        Session session = getSession();
        final Set<String> compounds = HippoNodeUtils.getCompounds(session);
        assertTrue(compounds.size() == 0);
        session.logout();
    }

    @Test(expected = Exception.class)
    public void testExceptionWhenNameIsEmpty() throws Exception {
        HippoNodeUtils.checkName("");
    }

    @Test(expected = Exception.class)
    public void testExceptionWhenNameContainsNumbers() throws Exception {
        HippoNodeUtils.checkName("aBc2e");
    }

    @Test(expected = Exception.class)
    public void testExceptionWhenUriIsEmpty() throws Exception {
        HippoNodeUtils.checkURI("");
    }

    @Test(expected = Exception.class)
    public void testExceptionWhenInvalidURI() throws Exception {
        HippoNodeUtils.checkURI("htp://invalidUrl");
    }

}
