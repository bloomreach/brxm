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
package org.hippoecm.repository.sample;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class Initialize
{
  protected final Logger log = LoggerFactory.getLogger(Initialize.class);

  HippoRepository repository;

  private Initialize() throws RepositoryException {
    repository = HippoRepositoryFactory.getHippoRepository();
  }

  private Initialize(String location) throws RepositoryException {
    repository = HippoRepositoryFactory.getHippoRepository(location);
  }

  private void initializeRepository() throws RepositoryException {
    Session session = repository.login("dummy", "dummy".toCharArray());
    Node node = session.getRootNode();
    if(!node.hasNode("configuration"))
      node = node.addNode("configuration","hippo:configuration");
    else
      node = node.getNode("configuration");
    if(!node.hasNode("initialize"))
      node = node.addNode("initialize","hippo:initializefolder");
    else
      node = node.getNode("initialize");
    node = node.addNode("newsmodel","hippo:initializeitem");
    node.setProperty("nodetypes","newsmodel.cnd");
    node.setProperty("content","navigation.xml");
    session.logout();
  }

  public static void main(String[] args) {
    Initialize bootstrap = null;
    try {
      String location = args.length>0 ? args[0] : "rmi://localhost:1099/jackrabbit.repository";
      if(location != null)
        bootstrap = new Initialize(location);
      else
        bootstrap = new Initialize();
      bootstrap.initializeRepository();
    } catch(RepositoryException ex) {
      System.err.println("RepositoryException: "+ex.getMessage());
      ex.printStackTrace(System.err);
    }
  }
}
