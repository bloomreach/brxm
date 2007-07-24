/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
package org.hippocms.repository.jr.servicing;

import javax.jcr.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream; 
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.JDOHelper;
import javax.jdo.Transaction;

import org.jpox.store.OID; 

import org.hippocms.repository.pojo.JCROID;
import org.hippocms.repository.jr.servicing.Document;

public class DocumentManagerImpl
  implements DocumentManager
{
  Session session;
  PersistenceManager pm;
  public DocumentManagerImpl(Session session) {
    try {
      this.session = session;
      Properties properties = new Properties();
      InputStream istream = getClass().getClassLoader().getResourceAsStream("jdo.properties");
      properties.load(istream);
      properties.setProperty("javax.jdo.option.ConnectionURL", "jcr:file:" + System.getProperty("user.dir")); // FIXME
      PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
      pm = pmf.getPersistenceManager();
    } catch(IOException ex) {
      // FIXME
    }
  }
  public Document getDocument(String category, String identifier) throws RepositoryException {
    try {
      String uuid = session.getRootNode().getNode("files").getNode(category).getNode(identifier).getUUID();
      Document obj = (Document) pm.getObjectById(new JCROID(uuid));
      return obj;
    } catch(UnsupportedRepositoryOperationException ex) {
      // FIXME
      return null;
    } catch(PathNotFoundException ex) {
      /* getDocument cannot and should not be used to create documents.
       * null is a valid way to check whether the document looked for exist.
       */
      return null;
    }
  }
}
