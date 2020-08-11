/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class LocalRefreshTest extends RepositoryTestCase {

    @Test
    public void testLocalRefreshDiscardsChanges() throws RepositoryException {
        session.getRootNode().addNode("foobar");
        assertTrue(session.nodeExists("/foobar"));

        HippoSession hippoSession = (HippoSession) session;
        hippoSession.localRefresh();
        assertFalse(session.nodeExists("/foobar"));

        session.refresh(false);
    }

    @Test
    public void testLocalRefreshFlushesVirtualStates() throws RepositoryException {
        final String[] contents = new String[] {
                "/test", "nt:unstructured",
                "/test/foo", "nt:unstructured",
                "/test/foo/bar", "nt:unstructured"
        };
        build(contents, session);
        final Node test = session.getNode("/test");
        final Node foo = session.getNode("/test/foo");
        final Node mirror = test.addNode("mirror", "hippo:mirror");
        mirror.setProperty("hippo:docbase", foo.getIdentifier());
        session.save();

        assertTrue(session.nodeExists("/test/mirror/bar"));

        final Session session2 = session.impersonate(new SimpleCredentials("admin", new char[] {}));
        session2.getNode("/test/foo").addNode("baz");
        session2.save();
        session2.logout();

        assertFalse(session.nodeExists("/test/mirror/baz"));

        HippoSession hippoSession = (HippoSession) session;
        hippoSession.localRefresh();
        assertTrue(session.nodeExists("/test/mirror/baz"));
    }

}
