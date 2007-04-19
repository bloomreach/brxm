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
package org.hippocms.repository.jr.embedded;

import java.lang.Object;
import java.lang.String;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import java.security.AccessControlException;

import javax.jcr.*;
import javax.jcr.query.*;
import javax.jcr.version.*;
import javax.jcr.lock.*;
import javax.jcr.nodetype.*;

/**
 * @version $Id$
 *
 */
class VirtualSessionImpl implements Session
{
  protected Session actual;
  VirtualSessionImpl(Session actual) {
    this.actual = actual;
  }
  public Repository getRepository() {
    return actual.getRepository();
  }
  public String getUserID() {
    return actual.getUserID();
  }
  public Object getAttribute(String name) {
    return actual.getAttribute(name);
  }
  public String[] getAttributeNames() {
    return actual.getAttributeNames();
  }
  public Workspace getWorkspace() {
    return actual.getWorkspace();
  }
  public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
    return new VirtualSessionImpl(actual.impersonate(credentials));
  }
  public Node getRootNode() throws RepositoryException {
    return new VirtualNodeImpl(actual.getRootNode());
  }
  public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
    return new VirtualNodeImpl(actual.getNodeByUUID(uuid));
  }
  public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
    Item item = actual.getItem(absPath);
    if(item.isNode())
      return new VirtualNodeImpl((Node)item);
    else
      return item;
  }
  public boolean itemExists(String absPath) throws RepositoryException {
    return actual.itemExists(absPath);
  }
  public void move(String srcAbsPath, String destAbsPath)
    throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
           RepositoryException {
    actual.move(srcAbsPath, destAbsPath);
  }
  public void save()
    throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
           InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    actual.save();
  }
  public void refresh(boolean keepChanges) throws RepositoryException {
    actual.refresh(keepChanges);
  }
  public boolean hasPendingChanges() throws RepositoryException {
    return actual.hasPendingChanges();
  }
  public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getValueFactory();
  }
  public void checkPermission(String absPath, String actions)
    throws AccessControlException, RepositoryException {
    actual.checkPermission(absPath, actions);
  }
  public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior)
    throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
    return actual.getImportContentHandler(parentAbsPath, uuidBehavior);
  }
  public void importXML(String parentAbsPath, InputStream in, int uuidBehavior)
    throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
           VersionException, InvalidSerializedDataException, LockException, RepositoryException {
    actual.importXML(parentAbsPath, in, uuidBehavior);
  }
  public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
    throws PathNotFoundException, SAXException, RepositoryException {
    actual.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
  }
  public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
    throws IOException, PathNotFoundException, RepositoryException {
    actual.exportSystemView(absPath, out, skipBinary, noRecurse);
  }
  public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
    throws PathNotFoundException, SAXException, RepositoryException {
    actual.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
  }
  public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
    throws IOException, PathNotFoundException, RepositoryException {
    actual.exportDocumentView(absPath, out, skipBinary, noRecurse);
  }
  public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
    actual.setNamespacePrefix(prefix, uri);
  }
  public String[] getNamespacePrefixes() throws RepositoryException {
    return actual.getNamespacePrefixes();
  }
  public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
    return actual.getNamespaceURI(prefix);
  }
  public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
    return actual.getNamespacePrefix(uri);
  }
  public void logout() {
    actual.logout();
  }
  public boolean isLive() {
    return actual.isLive();
  }
  public void addLockToken(String lt) {
    actual.addLockToken(lt);
  }
  public String[] getLockTokens() {
    return actual.getLockTokens();
  }
  public void removeLockToken(String lt) {
    actual.removeLockToken(lt);
  }
}
