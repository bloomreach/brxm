/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.sample;

import java.io.IOException;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.hippoecm.repository.HippoRepository;

abstract class SampleWorkflowSetup
{
  static int oldAuthorId;
  static int newAuthorId;

  static void commonStart(HippoRepository server) throws RepositoryException, IOException {
    Random rnd = new Random(8675687);
    oldAuthorId = rnd.nextInt();
    newAuthorId = rnd.nextInt();

    Session session = server.login("systemuser","systempass".toCharArray());
    Node root = session.getRootNode();

    // set up the workflow specification as a node "/configuration/hippo:workflows/mycategory/myworkflow"
    Node node = root.getNode("configuration");
    node = node.addNode("hippo:workflows","hippo:workflowfolder");
    node.addMixin("mix:referenceable");
    node = node.addNode("mycategory","hippo:workflowcategory");
    node = node.addNode("myworkflow","hippo:workflow");
    node.setProperty("hippo:nodetype","hippo:newsArticle");
    node.setProperty("hippo:display","Sample Workflow");
    node.setProperty("hippo:renderer","org.hippoecm.repository.sample.SampleWorkflowRenderer");
    node.setProperty("hippo:classname","org.hippoecm.repository.sample.SampleWorkflowImpl");
    Node types = node.getNode("hippo:types");
    node = types.addNode("org.hippoecm.repository.sample.AuthorDocument","hippo:type");
    node.setProperty("hippo:nodetype","hippo:author");
    node.setProperty("hippo:display","AuthorDocument");
    node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");
    node = types.addNode("org.hippoecm.repository.sample.ArticleDocument","hippo:type");
    node.setProperty("hippo:nodetype","hippo:newsArticle");
    node.setProperty("hippo:display","ArticleDocument");
    node.setProperty("hippo:classname","org.hippoecm.repository.sample.ArticleDocument");

    // set up the queryable document specification as a node "/configuration/hippo:documents/authors"
    node = root.getNode("configuration");
    node = node.addNode("hippo:documents","hippo:queryfolder");
    node.addMixin("mix:referenceable");
    node = node.addNode("authors","hippo:query");
    node.setProperty("hippo:query","files//*[@jcr:primaryType='hippo:author' and @hippo:name='?']");
    node.setProperty("hippo:language",Query.XPATH);
    node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");
    node = node.getNode("hippo:types");
    node = node.addNode("org.hippoecm.repository.sample.AuthorDocument","hippo:type");
    node.setProperty("hippo:nodetype","hippo:author");
    node.setProperty("hippo:display","AuthorDocument");
    node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");

    root.addNode("files");

    node = root.getNode("files");
    node = node.addNode("myauthor","hippo:author");
    node.setProperty("hippo:id",newAuthorId);
    node.setProperty("hippo:name","Jan Smit");

    node = root.getNode("files");
    node = node.addNode("myarticle","hippo:newsArticle");
    node.setProperty("hippo:id",1);
    node.setProperty("hippo:authorId",oldAuthorId);

    session.save();
    session.logout();
  }

  static void commonEnd(HippoRepository server) throws RepositoryException {
    Session session = server.login("dummy","dummy".toCharArray());
    Node root = session.getRootNode();
    root.getNode("files").remove();
    Node node = root.getNode("configuration");
    node.getNode("hippo:workflows").remove();
    node.getNode("hippo:documents").remove();
    session.save();
    session.logout();
  }
}
