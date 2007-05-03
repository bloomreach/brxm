/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.servicing;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

public class ServicesManagerImpl
  implements ServicesManager
{
  class Entry {
    ServiceImpl service;
    Node node;
    Entry(ServiceImpl service, Node node) {
      this.service = service;
      this.node    = node;
    }
  }
  Session session;
  private List<Entry> usedServices;
  public ServicesManagerImpl(Session session) {
    this.session = session;
    usedServices = new LinkedList<Entry>();
  }
  public Service getService(Node node) throws RepositoryException {
    try {
      ServiceImpl service = new ServiceImpl();
      service.setAction1(node.getProperty("HasAction1").getBoolean());
      service.setAction2(node.getProperty("HasAction2").getBoolean());
      usedServices.add(new Entry(service, node));
      return service;
    } catch(RemoteException ex) {
      throw new RepositoryException("service inaccessible", ex);
    }
  }
  void save(ServiceImpl service, Node node) throws RepositoryException {
    node.setProperty("HasAction1",service.getAction1());
    node.setProperty("HasAction2",service.getAction2());
  }
  public void save() throws RepositoryException {
    for(Iterator<Entry> iter = usedServices.iterator(); iter.hasNext(); ) {
      Entry entry = iter.next();
      save(entry.service, entry.node);
    }
    // FIXME: this assumes that Services are no longer used after a session.save()
    usedServices.clear();
  }
  public Session getSession() throws RepositoryException {
    return session;
  }
}
