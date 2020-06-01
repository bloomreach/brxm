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

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.HippoRepository;

abstract class SampleWorkflowSetup
{

    static int oldAuthorId;
    static int newAuthorId;

    static void commonStart(HippoRepository server) throws RepositoryException {
        Random rnd = new Random(8675687);
        oldAuthorId = rnd.nextInt();
        newAuthorId = rnd.nextInt();

        Session session = server.login("admin","admin".toCharArray());
        Node root = session.getRootNode();

        // set up the workflow specification as a node "/hippo:configuration/hippo:workflows/mycategory/myworkflow"
        Node node = root.getNode("hippo:configuration");
        node = node.getNode("hippo:workflows");
        node = node.addNode("mycategory","hipposys:workflowcategory");
        node = node.addNode("myworkflow", "hipposys:workflow");
        node.setProperty("hipposys:nodetype","sample:newsArticle");
        node.setProperty("hipposys:display","Sample Workflow");
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.SampleWorkflowImpl");

        root.addNode("files");

        node = root.getNode("files");
        node = node.addNode("myauthor","sample:author");
        node.addMixin("mix:versionable");
        node.setProperty("sample:id",newAuthorId);
        node.setProperty("sample:name","Jan Smit");

        node = root.getNode("files");
        node = node.addNode("myarticle","sample:newsArticle");
        node.addMixin("mix:versionable");
        node.setProperty("sample:id",1);
        node.setProperty("sample:authorId",oldAuthorId);

        session.save();
        session.logout();
    }

    static void commonEnd(HippoRepository server) throws RepositoryException {
        Session session = server.login("admin","admin".toCharArray());
        Node root = session.getRootNode();
        root.getNode("files").remove();

        Node workflows = root.getNode("hippo:configuration/hippo:workflows");
        if (workflows.hasNode("mycategory")) {
            workflows.getNode("mycategory").remove();
        }

        session.save();
        session.logout();
    }
}
