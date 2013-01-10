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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.hippoecm.repository.api.HippoNodeType;

import org.junit.Ignore;
import org.junit.Test;

public class RefreshTest {

    @Ignore
    public void testRefreshAfterRemoveIssueJackrabbit() throws Exception {
        Repository repository = RepositoryImpl.create(RepositoryConfig.create(getClass().getResourceAsStream("jackrabbit.xml"), "target"));
        testRefreshAfterRemoveIssue(repository);
    }

    @Test
    public void testRefreshAfterRemoveIssueHippo() throws Exception {
        Repository repository = HippoRepositoryFactory.getHippoRepository().getRepository();
        testRefreshAfterRemoveIssue(repository);
    }

    @Ignore
    public void testMultiSessionMoveJackrabbit() throws Exception {
        Repository repository = RepositoryImpl.create(RepositoryConfig.create(getClass().getResourceAsStream("jackrabbit.xml"), "target"));
        testMultiSessionMove(repository);
    }

    @Test
    public void testMultiSessionMoveHippo() throws Exception {
        Repository repository = HippoRepositoryFactory.getHippoRepository().getRepository();
        testMultiSessionMove(repository);
    }

    private void testMultiSessionMove(Repository repository) throws Exception {
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        Node testNode = session.getRootNode().addNode("test");
        testNode.addNode("A").addNode("B");
        testNode.addNode("B");
        testNode.addNode("C");
        session.save();
        session.move("/test/A/B", "/test/C/B");
        Session altSession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        altSession.getRootNode().getNode("test/A").addNode("D");
        session.save();
        altSession.save();
        altSession.logout();
        session.getRootNode().getNode("test").remove();
        session.save();
    }

    protected void traverse(Node node) throws RepositoryException {
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!"jcr:system".equals(child.getName())) {
                traverse(child);
            }
        }
    }

    private void testRefreshAfterRemoveIssue(Repository repository) throws Exception {
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        session.getRootNode().addNode("test", "nt:unstructured");
        session.getRootNode().getNode("test").addNode("docs", "nt:unstructured").addMixin("mix:versionable");
        session.save();
        session.logout();
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        session.getRootNode().getNode("test").remove();
        session.refresh(false);
        session.save();
    }

}
