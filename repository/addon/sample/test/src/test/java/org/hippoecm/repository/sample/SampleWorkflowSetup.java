/*
 *  Copyright 2008 Hippo.
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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        node = node.addNode("myworkflow", "frontend:workflow");
        node.setProperty("hipposys:nodetype","sample:newsArticle");
        node.setProperty("hipposys:display","Sample Workflow");
        node.setProperty("frontend:renderer", "org.hippoecm.repository.sample.SampleWorkflowRenderer");
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.SampleWorkflowImpl");
        Node types = node.getNode("hipposys:types");
        node = types.addNode("org.hippoecm.repository.sample.AuthorDocument","hipposys:type");
        node.setProperty("hipposys:nodetype","sample:author");
        node.setProperty("hipposys:display","AuthorDocument");
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.AuthorDocument");
        node = types.addNode("org.hippoecm.repository.sample.ArticleDocument","hipposys:type");
        node.setProperty("hipposys:nodetype","sample:newsArticle");
        node.setProperty("hipposys:display","ArticleDocument");
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.ArticleDocument");

        // set up the queryable document specification as a node "/configuration/hippo:documents/authors"
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("files//*[@jcr:primaryType='sample:author' and @sample:name='$name']", Query.XPATH);
        node = query.storeAsNode("/hippo:configuration/hippo:documents/authors");
        String statement = node.getProperty("jcr:statement").getString();
        String language = node.getProperty("jcr:language").getString();
        node.remove();
        node = root.getNode("hippo:configuration/hippo:documents");
        node = node.addNode("authors","hipposys:ocmquery");
        node.setProperty("jcr:statement",statement);
        node.setProperty("jcr:language",language);
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.AuthorDocument");
        node = node.getNode("hipposys:types");
        node = node.addNode("org.hippoecm.repository.sample.AuthorDocument","hipposys:type");
        node.setProperty("hipposys:nodetype","sample:author");
        node.setProperty("hipposys:display","AuthorDocument");
        node.setProperty("hipposys:classname","org.hippoecm.repository.sample.AuthorDocument");

        root.addNode("files");

        node = root.getNode("files");
        node = node.addNode("myauthor","sample:author");
        node.addMixin("hippo:harddocument");
        node.setProperty("sample:id",newAuthorId);
        node.setProperty("sample:name","Jan Smit");

        node = root.getNode("files");
        node = node.addNode("myarticle","sample:newsArticle");
        node.addMixin("hippo:harddocument");
        node.setProperty("sample:id",1);
        node.setProperty("sample:authorId",oldAuthorId);

        session.save();
        session.logout();
    }

    static void commonEnd(HippoRepository server) throws RepositoryException {
        Session session = server.login("admin","admin".toCharArray());
        Node root = session.getRootNode();
        root.getNode("files").remove();

        root.getNode("hippo:configuration/hippo:workflows").remove();
        root.addNode("hippo:configuration/hippo:workflows", "hipposys:workflowfolder");

        root.getNode("hippo:configuration/hippo:documents").remove();
        root.addNode("hippo:configuration/hippo:documents", "hipposys:ocmqueryfolder");
        session.save();
        session.logout();
    }
}
