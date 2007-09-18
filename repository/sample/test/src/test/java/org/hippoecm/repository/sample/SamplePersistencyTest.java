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
import com.atomikos.icatch.jta.UserTransactionManager;

public class SamplePersistencyTest extends TestCase
{
  private HippoRepository server;

  public void setUp() throws RepositoryException, IOException {
    server = HippoRepositoryFactory.getHippoRepository();
    SampleWorkflowSetup.commonStart(server);
  }

  public void tearDown() throws RepositoryException {
    SampleWorkflowSetup.commonEnd(server);
    server.close();
  }

  public void testAuthorDocument() throws RepositoryException {
    Session session = server.login("dummy","dummy".toCharArray());
    Node root = session.getRootNode();

    DocumentManager manager = ((HippoWorkspace)session.getWorkspace()).getDocumentManager();
    Document document = manager.getDocument("authors","Jan Smit");

    assertTrue(document instanceof AuthorDocument);
    AuthorDocument author = (AuthorDocument) document;
    assertTrue(author.authorId == SampleWorkflowSetup.newAuthorId);

    session.logout();
  }
}
