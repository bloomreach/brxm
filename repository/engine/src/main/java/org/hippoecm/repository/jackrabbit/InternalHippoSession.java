/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.security.HippoAccessManager;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.ImportContext;

/**
 * Internal hippo session implementation methods.
 * The session class hierarchy branches before it reaches the hippo specific implementations.
 */
public interface InternalHippoSession extends JackrabbitSession, NamespaceResolver, NamePathResolver, IdentifierResolver {

    Subject getSubject();

    SessionUser getUser();

    /**
     *  <p>
     *      {@code true} when this {@link InternalHippoSession} is a JCR System Session, implying jcr:all everywhere.
     *      Mind you that {@link User#isSystemUser()  this.getUser().isSystemUser()} is something COMPLETELY different:
     *      that returns whether the user is a user required by the system, not whether the user is a JCR System Session
     *  </p>
     *
     * @return {@code true} when this {@link InternalHippoSession} is a JCR System Session, implying jcr:all everywhere.
     */
    boolean isSystemUser();

    NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws RepositoryException;

    void importEnhancedSystemViewXML(ImportContext importContext) throws IOException, RepositoryException;

    Node getCanonicalNode(Node node) throws RepositoryException;

    HippoSessionItemStateManager getItemStateManager();

    AuthorizationQuery getAuthorizationQuery();

    /**
     * sets an explicit authorization query, only for TESTING purposes
     */
    void setAuthorizationQuery(AuthorizationQuery authorizationQuery);

    Session createDelegatedSession(InternalHippoSession session, DomainRuleExtension... domainExtensions) throws RepositoryException;

    void localRefresh();

    NodeImpl getNodeById(NodeId id) throws ItemNotFoundException, RepositoryException;

    HierarchyManager getHierarchyManager();

    @Override
    HippoAccessManager getAccessControlManager() throws RepositoryException;

    ItemManager getItemManager();

    ScheduledExecutorService getExecutor();
}
