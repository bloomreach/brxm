/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.xml.sax.ContentHandler;

/**
 * Internal hippo session implementation methods.
 * The session class hierarchy branches before it reaches the hippo specific implementations.
 */
public interface InternalHippoSession extends JackrabbitSession, NamespaceResolver, NamePathResolver, IdentifierResolver {

    Subject getSubject();

    NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws RepositoryException;

    ContentHandler getDereferencedImportContentHandler(String parentAbsPath, int uuidBehavior, int referenceBehavior, int mergeBehavior) throws RepositoryException;

    void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior, int referenceBehavior, int mergeBehavior) throws IOException, RepositoryException;

    void importEnhancedSystemViewBinaryPackage(String parentAbsPath, File archive, int uuidBehaviour, int referenceBehaviour, int mergeBehaviour) throws IOException, RepositoryException;

    Node getCanonicalNode(Node node) throws RepositoryException;

    HippoSessionItemStateManager getItemStateManager();

    void registerSessionCloseCallback(HippoSession.CloseCallback callback);

    AuthorizationQuery getAuthorizationQuery();

    Session createDelegatedSession(InternalHippoSession session, DomainRuleExtension... domainExtensions) throws RepositoryException;

    void localRefresh();

}
