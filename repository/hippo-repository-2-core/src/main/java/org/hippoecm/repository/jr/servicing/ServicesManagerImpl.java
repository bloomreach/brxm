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
package org.hippoecm.repository.jr.servicing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ServicesManagerImpl
  implements ServicesManager
{
  class Entry {
    ServiceImpl service;
    String uuid;
    Entry(ServiceImpl service, String uuid) {
      this.service = service;
      this.uuid    = uuid;
    }
  }
  Session session;
  private List<Entry> usedServices;
  ClassLoader classloader;
  public ServicesManagerImpl(Session session) {
    this.session = session;
    usedServices = new LinkedList<Entry>();
    classloader = getClass().getClassLoader();
  }
  public Service getService(Node node) throws RepositoryException {
    return getService(node, null);
  }

  /**
   * @see DocumentManagerImpl.getObject(String,String,Node)
   */
  public Service getService(String uuid, String serviceName, Node types) throws RepositoryException {
    DocumentManagerImpl manager = (DocumentManagerImpl)((ServicingWorkspace)session.getWorkspace()).getDocumentManager();
    Object object = manager.getObject(uuid, serviceName, types);
    ServiceImpl service = (ServiceImpl) object;
    usedServices.add(new Entry(service, uuid));
    return service;
  }

  public Service getService(Node node, String serviceName) throws RepositoryException {
    if(serviceName == null)
      serviceName = "";
    return getService(node.getUUID(), serviceName, null);
  }
  void save(ServiceImpl service, String uuid) throws RepositoryException {
    DocumentManagerImpl documentManager = (DocumentManagerImpl)((ServicingWorkspace)session.getWorkspace()).getDocumentManager();
    documentManager.putObject(uuid, null, service);
  }
  void save() throws RepositoryException {
    for(Iterator<Entry> iter = usedServices.iterator(); iter.hasNext(); ) {
      Entry entry = iter.next();
    }
    for(Iterator<Entry> iter = usedServices.iterator(); iter.hasNext(); ) {
      Entry entry = iter.next();
      save(entry.service, entry.uuid);
    }
    // FIXME: this assumes that Services are no longer used after a session.save()
    usedServices.clear();
  }
  public Session getSession() throws RepositoryException {
    return session;
  }
}
