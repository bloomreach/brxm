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
package org.hippoecm.repository.sample;

import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.hippoecm.repository.RepositoryUrl;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.server.ServerServicingAdapterFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.sample.SampleWorkflowSetup.commonEnd;
import static org.hippoecm.repository.sample.SampleWorkflowSetup.commonStart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SampleRemoteWorkflowTest extends RepositoryTestCase {

    private RemoteRepository repository;
    private Registry registry;
    private Session session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        String bindingAddress = "rmi://localhost:1098/hipporepository";
        RepositoryUrl url = new RepositoryUrl(bindingAddress);
        repository = new ServerServicingAdapterFactory(url).getRemoteRepository(background.getRepository());

        // Get or start registry and bind the remote repository
        try {
            registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            registry.rebind(url.getName(), repository);
        } catch (ConnectException e) {
            registry = LocateRegistry.createRegistry(url.getPort());
            registry.rebind(url.getName(), repository);
        }
        commonStart(background);
        session = server.login("admin", "admin".toCharArray());
    }

    @After
    public void tearDown() throws Exception {
        commonEnd(background);
        if (session != null) {
            session.logout();
        }
    }

    @Test
    public void testWorkflow() throws Exception {

        Node node = session.getNode("/files/myarticle");
        assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.oldAuthorId);

        WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

        Workflow workflow = manager.getWorkflow("mycategory", node);
        assertNotNull("workflow not found", workflow);
        assertTrue("workflow not of proper type", workflow instanceof SampleWorkflow);
        ((SampleWorkflow) workflow).renameAuthor("Jan Smit");

        session.save();
        assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.newAuthorId);

    }

    @Test
    public void testReturnDocument() throws Exception {
        Node node = session.getNode("/files/myarticle");
        WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Workflow workflow = manager.getWorkflow("mycategory", node);
        assertNotNull(workflow);
        assertTrue("workflow not of proper type", workflow instanceof SampleWorkflow);
        Document document = ((SampleWorkflow) workflow).getArticle();
        assertTrue(node.getIdentifier().equals(document.getIdentity()));
    }
}
