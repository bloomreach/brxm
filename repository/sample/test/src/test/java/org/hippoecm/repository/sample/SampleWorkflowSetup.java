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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;

import org.apache.jackrabbit.core.XASession;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.RollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.xa.XAResource;

import java.util.Random;

abstract class SampleWorkflowSetup
{
  static int oldAuthorId;
  static int newAuthorId;

  static void commonStart(HippoRepository server) throws RepositoryException, IOException {
    Random rnd = new Random(8675687);
    oldAuthorId = rnd.nextInt();
    newAuthorId = rnd.nextInt();

    Session session = server.login("dummy","dummy".toCharArray());
    Node root = session.getRootNode();

    // set up the workflow specification as a node "/configuration/workflows/mycategory/myworkflow"
    Node node = root.getNode("configuration");
    node = node.addNode("workflows");
    node.addMixin("mix:referenceable");
    node = node.addNode("mycategory");
    node = node.addNode("myworkflow");
    node.setProperty("nodetype","hippo:newsArticle");
    node.setProperty("display","Sample Workflow");
    node.setProperty("renderer","org.hippoecm.repository.sample.SampleWorkflowRenderer");
    node.setProperty("service","org.hippoecm.repository.sample.SampleWorkflowImpl");
    Node types = node.addNode("types");
    node = types.addNode("org.hippoecm.repository.sample.AuthorDocument");
    node.setProperty("nodetype","hippo:author");
    node.setProperty("display","AuthorDocument");
    node.setProperty("classname","org.hippoecm.repository.sample.AuthorDocument");
    node = types.addNode("org.hippoecm.repository.sample.ArticleDocument");
    node.setProperty("nodetype","hippo:newsArticle");
    node.setProperty("display","ArticleDocument");
    node.setProperty("classname","org.hippoecm.repository.sample.ArticleDocument");

    // set up the queryable document specification as a node "/configuration/documents/authors"
    node = root.getNode("configuration");
    node = node.addNode("documents");
    node.addMixin("mix:referenceable");
    node = node.addNode("authors");
    node.setProperty("query","files//*[@jcr:primaryType='hippo:author' and @hippo:name='?']");
    node.setProperty("language",Query.XPATH);
    node.setProperty("classname","org.hippoecm.repository.sample.AuthorDocument");
    node = node.addNode("types");
    node = node.addNode("org.hippoecm.repository.sample.AuthorDocument");
    node.setProperty("nodetype","hippo:author");
    node.setProperty("display","AuthorDocument");
    node.setProperty("classname","org.hippoecm.repository.sample.AuthorDocument");

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
    node.getNode("workflows").remove();
    node.getNode("documents").remove();
    session.save();
    session.logout();
  }
}
