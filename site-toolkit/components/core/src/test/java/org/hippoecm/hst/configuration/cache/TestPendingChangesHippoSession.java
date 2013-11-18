/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test will fail after REPO-684 is fixed
 */
public class TestPendingChangesHippoSession extends AbstractHstLoadingCacheTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void assertPendingChangescorrectForMovesWithinSameParent() throws RepositoryException {

        Session session = createSession();

        session.move("/hst:hst/hst:configurations/unittestcommon/hst:components/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:components/header2");

        final NodeIterator nodeIterator = ((HippoSession) session).pendingChanges();
        while(nodeIterator.hasNext()) {
            if (nodeIterator.nextNode().getPath().equals("/hst:hst")) {
                fail("Should be no pending change for /hst:hst");
            }
        }

        session.logout();

    }

    @Test
    public void assertPendingChangesIncorrectForMovesBetweenDifferentParents() throws RepositoryException {
        Session session = createSession();
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage/header");

        final NodeIterator nodeIterator = ((HippoSession) session).pendingChanges();
        boolean expectedWrongPendingChangeFound = false;
        while (nodeIterator.hasNext()) {
            if (nodeIterator.nextNode().getPath().equals("/hst:hst")) {
                expectedWrongPendingChangeFound = true;
            }
        }
        assertTrue("Expected HippoSession pending changes to return incorrect pending change /hst:hst", expectedWrongPendingChangeFound);
        session.logout();

    }

    @Test
    public void assertPendingChangesCorrectForCopyDelete() throws RepositoryException {
        // instead of move, use copy / delete and assert pending changes are then correct
        Session session = createSession();
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage/header");

        session.removeItem("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header");

        final NodeIterator nodeIterator = ((HippoSession) session).pendingChanges();

        while(nodeIterator.hasNext()) {
            if (nodeIterator.nextNode().getPath().equals("/hst:hst")) {
                fail("Should be no pending change for /hst:hst");
            }
        }
        session.logout();
    }

    @Test
    public void assertPendingChangesCorrectForReordering()  throws RepositoryException {
        Session session = createSession();
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header1");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header2");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header3");

        session.save();

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage").orderBefore("header2", "header1");

        final NodeIterator nodeIterator = ((HippoSession) session).pendingChanges();

        while(nodeIterator.hasNext()) {
            // should be just one pending change for basepage
            assertEquals("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage", nodeIterator.nextNode().getPath());
        }

        session.removeItem("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header1");
        session.removeItem("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header2");
        session.removeItem("/hst:hst/hst:configurations/unittestcommon/hst:pages/basepage/header3");
        session.save();
        session.logout();
    }
}
