/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkflowEventTest extends RepositoryTestCase {

    private Node root;
    private WorkflowManager manager;

    private String[] content = {
        "/test/counter",                  "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/counter/counter",          "hippo:wfeventcounter",
        "jcr:mixinTypes",                 "mix:versionable",
        "hippo:counter",                  "1",

        "/test/folder",                   "hippostd:folder",
        "jcr:mixinTypes",                 "mix:versionable",
        "/test/folder/document",          "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/folder/document/document", "hippo:document",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/target",                   "hippostd:folder",
        "jcr:mixinTypes",                 "mix:versionable",
        "/hippo:configuration/hippo:queries/hippo:templates/test", "hippostd:templatequery",
        "jcr:mixinTypes",                 "hipposys:implementation",
        "hipposys:classname",             "org.hippoecm.repository.impl.query.DirectPath",
        "hippostd:modify",                "./_name",
        "hippostd:modify",                "$name",
        "hippostd:modify",                "./_node/_name",
        "hippostd:modify",                "$name",
        "jcr:language",                   "xpath",
        "jcr:statement",                  "/jcr:root/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/node()",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates", "hippostd:templates",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/prototype", "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/prototype/prototype", "hippo:wfeventdocument",
        "jcr:mixinTypes",                 "mix:versionable",
        "hippo:counter",                  "0",

        "/hippo:configuration/hippo:workflows/postprocess", "hipposys:workflowcategory",
        "/hippo:configuration/hippo:workflows/postprocess/create", "hipposys:workflow",
        "hipposys:nodetype", "hippo:wfeventdocument",
        "hipposys:display", "workflow events document",
        "hipposys:classname", "org.hippoecm.repository.test.PostProcessWorkflowImpl",

        "/hippo:configuration/hippo:workflows/events", "hipposys:workflowcategory",
        "/hippo:configuration/hippo:workflows/events/test", "hipposys:workflowsimplequeryevent",
        "hipposys:nodetype", "hippostd:folder",
        "hipposys:display", "workflow events test",
        "hipposys:classname", "org.hippoecm.repository.test.WorkflowEventWorkflowImpl",
        "hipposys:eventconditionoperator", "post\\pre",
        "hipposys:eventdocument", "/test/counter/counter",
        "/hippo:configuration/hippo:workflows/events/test/hipposys:eventprecondition", "nt:query",
        "jcr:mixinTypes",                 "mix:referenceable",
        "jcr:language", "JCR-SQL2",
        "jcr:statement", "SELECT child.[jcr:uuid] AS id FROM [hippo:hardhandle] AS child INNER JOIN [hippo:document] AS parent ON ISCHILDNODE(child,parent) WHERE parent.[jcr:uuid] = $subject",
        "/hippo:configuration/hippo:workflows/events/test/hipposys:eventpostcondition", "nt:query",
        "jcr:mixinTypes",                 "mix:referenceable",
        "jcr:language", "JCR-SQL2",
        "jcr:statement", "SELECT child.[jcr:uuid] AS id FROM [hippo:hardhandle] AS child INNER JOIN [hippo:document] AS parent ON ISCHILDNODE(child,parent) WHERE parent.[jcr:uuid] = $subject"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode("hippo:configuration/hippo:queries/hippo:templates/test")) {
            session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates/test").remove();
        }
        root = session.getRootNode().addNode("test");
        session.save();
        build(content, session);
        session.save();
        manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (session.getRootNode().hasNode("hippo:configuration/hippo:queries/hippo:templates/test")) {
            session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates/test").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:workflows/events")) {
            session.getRootNode().getNode("hippo:configuration/hippo:workflows/events").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:workflows/postprocess")) {
            session.getRootNode().getNode("hippo:configuration/hippo:workflows/postprocess").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testEventFire() throws RepositoryException, WorkflowException, RemoteException {
        Node folder = root.getNode("folder");
        assertEquals(1L, root.getProperty("counter/counter/hippo:counter").getLong());
        {
            FolderWorkflow workflow = (FolderWorkflow)manager.getWorkflow("threepane", folder);
            assertEquals(1L, root.getProperty("counter/counter/hippo:counter").getLong());
            String path = workflow.add("test", "prototype", "new");
            Node node = session.getNode(path);
            assertTrue(node.hasProperty("hippo:counter"));
            assertEquals(1L, node.getProperty("hippo:counter").getLong());
        } {
            FolderWorkflow workflow = (FolderWorkflow)manager.getWorkflow("threepane", folder);
            assertEquals(2L, root.getProperty("counter/counter/hippo:counter").getLong());
            String path = workflow.add("test", "prototype", "new");
            Node node = session.getNode(path);
            assertTrue(node.hasProperty("hippo:counter"));
            assertEquals(2L, node.getProperty("hippo:counter").getLong());
        }
        assertEquals(3L, root.getProperty("counter/counter/hippo:counter").getLong());
    }
}
