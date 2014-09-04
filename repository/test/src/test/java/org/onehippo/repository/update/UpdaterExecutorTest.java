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
package org.onehippo.repository.update;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UpdaterExecutorTest extends RepositoryTestCase {

    private final String[] content = {
            "/test", "nt:unstructured",
            "/test/foo", "nt:unstructured",
            "/test/foo/bar", "nt:unstructured",
            "/test/bar", "nt:unstructured",
            "/test/bar/foo", "nt:unstructured"
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        final Node registry = JcrUtils.getNodeIfExists("/hippo:configuration/hippo:update/hippo:registry", session);
        if (registry != null) {
            final NodeIterator updaters = registry.getNodes();
            while (updaters.hasNext()) {
                updaters.nextNode().remove();
            }
        }
        final Node test = JcrUtils.getNodeIfExists("/test", session);
        if (test != null) {
            final NodeIterator nodes = test.getNodes();
            while (nodes.hasNext()) {
                nodes.nextNode().remove();
            }
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testRunPathVisitorUpdate() throws Exception {
        testRunPathVisitor(false, false);
    }

    @Test
    public void testRunQueryVisitorUpdate() throws Exception {
        testRunQueryVisitor(false, false);
    }

    @Test
    public void testDryRunDoesNotSaveChanges() throws Exception {
        testRunQueryVisitor(false, true);
    }


    private void testRunPathVisitor(boolean revert, boolean dryRun) throws Exception {
        final String[] content = new String[] {
                "/hippo:configuration/hippo:update/hippo:registry/pathtest", "hipposys:updaterinfo",
                "hipposys:class", "org.onehippo.repository.update.UpdaterExecutorTest$TestUpdater",
                "hipposys:path", "/test"
        };
        build(content, session);
        final Node updaterNode = session.getNode("/hippo:configuration/hippo:update/hippo:registry/pathtest");
        updaterNode.setProperty("hipposys:revert", revert);
        updaterNode.setProperty("hipposys:dryrun", dryRun);
        session.save();

        final UpdaterExecutor updaterExecutor = new UpdaterExecutor(updaterNode, session);
        updaterExecutor.execute();
        updaterExecutor.destroy();

        assertEquals(2, JcrUtils.getLongProperty(updaterNode, "hipposys:updatedcount", -1L).longValue());
        assertEquals(1, JcrUtils.getLongProperty(updaterNode, "hipposys:failedcount", -1L).longValue());
        assertEquals(2, JcrUtils.getLongProperty(updaterNode, "hipposys:skippedcount", -1L).longValue());
        final String qux = JcrUtils.getStringProperty(session.getNode("/test/bar"), "qux", null);
        if (dryRun) {
            assertNull(qux);
        } else {
            assertEquals(revert ? "reverted" : "updated", qux);
        }
    }

    private void testRunQueryVisitor(boolean revert, boolean dryRun) throws Exception {
        final String[] content = new String[] {
                "/hippo:configuration/hippo:update/hippo:registry/querytest", "hipposys:updaterinfo",
                "hipposys:class", "org.onehippo.repository.update.UpdaterExecutorTest$TestUpdater",
                "hipposys:query", "/jcr:root/test//element(*, nt:unstructured)"
        };
        build(content, session);
        final Node updaterNode = session.getNode("/hippo:configuration/hippo:update/hippo:registry/querytest");
        updaterNode.setProperty("hipposys:revert", revert);
        updaterNode.setProperty("hipposys:dryrun", dryRun);
        session.save();

        final UpdaterExecutor updaterExecutor = new UpdaterExecutor(updaterNode, session);
        updaterExecutor.execute();
        updaterExecutor.destroy();
        assertEquals(2, JcrUtils.getLongProperty(updaterNode, "hipposys:updatedcount", -1L).longValue());
        assertEquals(1, JcrUtils.getLongProperty(updaterNode, "hipposys:failedcount", -1L).longValue());
        assertEquals(1, JcrUtils.getLongProperty(updaterNode, "hipposys:skippedcount", -1L).longValue());
        final String qux = JcrUtils.getStringProperty(session.getNode("/test/bar"), "qux", null);
        if (dryRun) {
            assertNull(qux);
        } else {
            assertEquals(revert ? "reverted" : "updated", qux);
        }
    }

    public static class TestUpdater extends BaseNodeUpdateVisitor {

        @Override
        public boolean doUpdate(final Node node) throws RepositoryException {
            if (shouldUpdate(node)) {
                node.setProperty("qux", "updated");
                return true;
            }
            return false;
        }

        @Override
        public boolean undoUpdate(final Node node) throws RepositoryException {
            if (shouldUpdate(node)) {
                node.setProperty("qux", "reverted");
                return true;
            }
            return false;
        }

        private boolean shouldUpdate(final Node node) throws RepositoryException {
            if (node.getPath().equals("/test/foo/bar")) {
                throw new RuntimeException("Deliberate exception for testing");
            }
            return node.getPath().startsWith("/test/bar");
        }

    }

}
