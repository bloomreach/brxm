/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.onehippo.repository.security.domain.DomainRuleExtension;

public class DelegatorAccessManager implements AccessManager {

    private final HippoAccessManager primary;
    private final HippoAccessManager secondary;

    public DelegatorAccessManager(final HippoAccessManager primary, final HippoAccessManager secondary, NamespaceResolver namespaceResolver,
                                  final DomainRuleExtension... domainExtensions) throws RepositoryException {
        this.primary = primary;
        this.secondary = secondary;
        primary.registerDomainRuleExtensions(namespaceResolver, domainExtensions);
        secondary.registerDomainRuleExtensions(namespaceResolver, domainExtensions);
    }

    @Override
    public void init(final AMContext context) throws AccessDeniedException, Exception {
    }

    @Override
    public void init(final AMContext context, final AccessControlProvider acProvider, final WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
    }

    @Override
    public void close() throws Exception {
        primary.close();
        secondary.close();
    }

    @Override
    public void checkPermission(final ItemId id, final int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        try {
            primary.checkPermission(id, permissions);
        } catch (AccessDeniedException e) {
            secondary.checkPermission(id, permissions);
        }
    }

    @Override
    public void checkPermission(final Path absPath, final int permissions) throws AccessDeniedException, RepositoryException {
        try {
            primary.checkPermission(absPath, permissions);
        } catch (AccessDeniedException e) {
            secondary.checkPermission(absPath, permissions);
        }
    }

    @Override
    public void checkRepositoryPermission(final int permissions) throws AccessDeniedException, RepositoryException {
        try {
            primary.checkRepositoryPermission(permissions);
        } catch (AccessDeniedException e) {
            secondary.checkRepositoryPermission(permissions);
        }
    }

    @Override
    public boolean isGranted(final ItemId id, final int permissions) throws ItemNotFoundException, RepositoryException {
        return primary.isGranted(id, permissions) || secondary.isGranted(id, permissions);
    }

    @Override
    public boolean isGranted(final Path absPath, final int permissions) throws RepositoryException {
        return primary.isGranted(absPath, permissions) || secondary.isGranted(absPath, permissions);
    }

    @Override
    public boolean isGranted(final Path parentPath, final Name childName, final int permissions) throws RepositoryException {
        return primary.isGranted(parentPath, childName, permissions) | secondary.isGranted(parentPath, childName, permissions);
    }

    @Override
    public boolean canRead(final Path itemPath, final ItemId itemId) throws RepositoryException {
        return primary.canRead(itemPath, itemId) || secondary.canRead(itemPath, itemId);
    }

    @Override
    public boolean canAccess(final String workspaceName) throws RepositoryException {
        return primary.canAccess(workspaceName) || secondary.canAccess(workspaceName);
    }
}
