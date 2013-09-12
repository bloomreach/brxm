/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.repository.upgrade;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.VersionManagerImpl;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

class HandleMigrator {

    private static final Logger log = LoggerFactory.getLogger(HandleMigrator.class);
    private static final String HANDLE_MIGRATION_WORKSPACE = "handlemigration";
    private static final String JCR_FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";
    private static final String JCR_FROZEN_MIXIN_TYPES = "jcr:frozenMixinTypes";
    private static final String NT_FROZEN_NODE = "nt:frozenNode";

    private final Session defaultSession;
    private Session migrationSession;
    private boolean cancelled = false;

    HandleMigrator(final Session session) {
        this.defaultSession = session;
    }

    void init() throws RepositoryException {
        createMigrationWorkspaceIfNotExists();
        migrationSession = loginToMigrationWorkspace();
    }

    void shutdown() {
        if (defaultSession != null) {
            defaultSession.logout();
        }
        if (migrationSession != null) {
            migrationSession.logout();
        }
    }

    private Iterable<Node> getHardHandles() {
        try {
            final QueryManager queryManager = defaultSession.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery("SELECT * FROM hippo:hardhandle", Query.SQL);
            return new NodeIterable(query.execute().getNodes());
        } catch (RepositoryException e) {
            log.error("Failed to query for handles to migrate", e);
        }
        return Collections.emptyList();
    }

    private boolean migrationPending() {
        return getHardHandles().iterator().hasNext();
    }

    void migrate() {
        log.debug("Running handle migration tool");

        if (migrationPending()) {
            try {
                init();
                int count = 0;
                for (Node handle : getHardHandles()) {
                    if (cancelled) {
                        break;
                    }
                    try {
                        migrate(handle);
                        count++;
                        throttle(count);
                    } catch (RepositoryException e) {
                        log.error("Failed to migrate " + JcrUtils.getNodePathQuietly(handle), e);
                    }
                }
                log.info("Migrated {} handles to new model", count);
            } catch (RepositoryException e) {
                log.error("Migration failed", e);
            } finally {
                if (migrationSession != null) {
                    migrationSession.logout();
                }
            }
        }

    }

    private void throttle(final int count) {
        if (count % 10 == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignore) {
            }
        } else {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }
    }

    void migrate(final Node handle) throws RepositoryException {
        log.debug("Migrating {}", handle.getPath());
        try {
            if (hasHistory(handle)) {
                migrateVersionHistory(handle);
            }
            migrateHandle(handle);
        } finally {
            defaultSession.refresh(false);
            migrationSession.refresh(false);
        }
    }

    private boolean hasHistory(final Node handle) throws RepositoryException {
        final VersionManager versionManager = defaultSession.getWorkspace().getVersionManager();
        final Version baseVersion = versionManager.getBaseVersion(handle.getPath());
        return !baseVersion.getName().equals("jcr:rootVersion");
    }

    private void migrateHandle(final Node handle) throws RepositoryException {
        log.debug("Migrating handle {}", handle.getPath());
        // we need to remove all incoming references here before we can remove the hard handle
        final Map<Node, String> references = removeReferences(handle);
        handle.removeMixin(HippoNodeType.NT_HARDHANDLE);
        handle.addMixin(JcrConstants.MIX_REFERENCEABLE);
        addReferences(handle, references);
        defaultSession.save();
    }

    private void addReferences(final Node handle, final Map<Node, String> references) throws RepositoryException {
        for (Map.Entry<Node, String> entry : references.entrySet()) {
            final Node node = entry.getKey();
            final String property = entry.getValue();
            node.setProperty(property, handle);
        }
    }

    private Map<Node, String> removeReferences(final Node handle) throws RepositoryException {
        final Map<Node, String> references = new HashMap<Node, String>();
        for (Property property : new PropertyIterable(handle.getReferences())) {
            final Node node = property.getParent();
            JcrUtils.ensureIsCheckedOut(node, true);
            final String propertyName = property.getName();
            if (!HippoNodeType.HIPPO_RELATED.equals(propertyName)) {
                references.put(node, propertyName);
            }
            property.remove();
        }
        return references;
    }

    private void migrateVersionHistory(final Node handle) throws RepositoryException {
        log.debug("Migrating version history of {}", handle.getPath());
        try {
            final Node tmp  = createTemporaryNode(handle);
            replayHistory(handle, tmp);
            replacePreview(handle, tmp.getIdentifier());
        } finally {
            removeTemporaryNode();
        }
    }

    private void replacePreview(final Node handle, final String identifier) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(handle, false);
        final String docPath = handle.getPath() + "/" + handle.getName();
        defaultSession.getWorkspace().clone(HANDLE_MIGRATION_WORKSPACE, "/tmp", docPath, true);
        final Node oldPreview = getOldPreview(handle, identifier);
        if (oldPreview != null) {
            final Node newPreview = defaultSession.getNodeByIdentifier(identifier);
            copy(oldPreview, newPreview);
            oldPreview.remove();
            defaultSession.save();
        }
    }

    private Node getOldPreview(final Node handle, final String identifier) throws RepositoryException {
        for (Node document : new NodeIterable(handle.getNodes(handle.getName()))) {
            final String state = JcrUtils.getStringProperty(document, HippoStdNodeType.HIPPOSTD_STATE, null);
            if (HippoStdNodeType.UNPUBLISHED.equals(state) && !document.getIdentifier().equals(identifier)) {
                return document;
            }
        }
        return null;
    }

    private void replayHistory(final Node handle, final Node tmp) throws RepositoryException {
        final VersionManagerImpl versionManager = (VersionManagerImpl)
                migrationSession.getWorkspace().getVersionManager();
        int count = 0;
        for (Version version : getVersions(handle)) {
            clear(tmp);
            copy(version.getFrozenNode(), tmp);
            tmp.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.UNPUBLISHED);
            migrationSession.save();
            versionManager.checkin("/tmp", version.getCreated());
            versionManager.checkout("/tmp");
            count++;
        }
        log.debug("Replayed {} versions", count);
    }

    private Node createTemporaryNode(final Node handle) throws RepositoryException {
        final Node document = handle.getNode(handle.getName());
        final String primaryType = document.getPrimaryNodeType().getName();
        final Node tmp = migrationSession.getRootNode().addNode("tmp", primaryType);
        tmp.addMixin(JcrConstants.MIX_VERSIONABLE);
        return tmp;
    }

    private void removeTemporaryNode() {
        try {
            while (migrationSession.nodeExists("/tmp")) {
                migrationSession.getNode("/tmp").remove();
            }
            migrationSession.save();
        } catch (RepositoryException e) {
            log.error("Error while removing temporary node", e);
        }
    }

    private void clear(final Node srcNode) throws RepositoryException {
        for (Property property : new PropertyIterable(srcNode.getProperties())) {
            if (!property.getDefinition().isProtected()) {
                property.remove();
            }
        }
        for (Node node : new NodeIterable(srcNode.getNodes())) {
            if (!node.getDefinition().isProtected()) {
                node.remove();
            }
        }
    }

    private void copy(final Node srcNode, final Node destNode) throws RepositoryException {

        for (String mixinType : getMixinTypesToCopy(srcNode)) {
            destNode.addMixin(mixinType);
        }

        for (Property property : new PropertyIterable(srcNode.getProperties())) {
            if (!excludeProperty(property.getName())) {
                if (property.isMultiple()) {
                    destNode.setProperty(property.getName(), property.getValues(), property.getType());
                } else {
                    destNode.setProperty(property.getName(), property.getValue(), property.getType());
                }
            }
        }

        for (Node srcChild : new NodeIterable(srcNode.getNodes())) {
            Node destChild;
            if (destNode.hasNode(srcChild.getName())) {
                destChild = destNode.getNode(srcChild.getName());
            } else {
                final String primaryNodeType = getPrimaryTypeToCopy(srcChild);
                destChild = destNode.addNode(srcChild.getName(), primaryNodeType);
            }
            copy(srcChild, destChild);
        }

    }

    private Iterable<String> getMixinTypesToCopy(final Node node) throws RepositoryException {
        final List<String> result = new ArrayList<String>();
        if (node.isNodeType(NT_FROZEN_NODE)) {
            if (node.hasProperty(JCR_FROZEN_MIXIN_TYPES)) {
                for (Value value : node.getProperty(JCR_FROZEN_MIXIN_TYPES).getValues()) {
                    result.add(value.getString());
                }
            }
        } else {
            for (NodeType nodeType : node.getMixinNodeTypes()) {
                result.add(nodeType.getName());
            }
        }
        return result;
    }

    private String getPrimaryTypeToCopy(final Node node) throws RepositoryException {
        if (node.isNodeType(NT_FROZEN_NODE)) {
            return node.getProperty(JCR_FROZEN_PRIMARY_TYPE).getString();
        } else {
            return node.getPrimaryNodeType().getName();
        }
    }

    private boolean excludeProperty(final String propertyName) {
        return propertyName.startsWith("jcr:") || propertyName.equals("hippo:related");
    }

    private List<Version> getVersions(final Node handle) throws RepositoryException {
        final List<Version> versions = new ArrayList<Version>();

        final VersionManager versionManager = defaultSession.getWorkspace().getVersionManager();
        final VersionHistory handleHistory = versionManager.getVersionHistory(handle.getPath());
        final VersionIterator allHandleVersions = handleHistory.getAllVersions();
        while (allHandleVersions.hasNext()) {
            final Version handleVersion = allHandleVersions.nextVersion();
            if (!handleVersion.getName().equals("jcr:rootVersion")) {
                final NodeIterator nodes = handleVersion.getNode("jcr:frozenNode").getNodes();
                while (nodes.hasNext()) {
                    final Node node = nodes.nextNode();
                    if (node.isNodeType("nt:versionedChild")) {
                        final String reference = node.getProperty("jcr:childVersionHistory").getString();
                        final VersionHistory documentHistory = (VersionHistory)
                                defaultSession.getNodeByIdentifier(reference);
                        final VersionIterator allDocumentVersions = documentHistory.getAllVersions();
                        while (allDocumentVersions.hasNext()) {
                            final Version documentVersion = allDocumentVersions.nextVersion();
                            if (!documentVersion.getName().equals("jcr:rootVersion")) {
                                versions.add(documentVersion);
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(versions, new Comparator<Version>() {
            @Override
            public int compare(final Version v1, final Version v2) {
                try {
                    final Long t1 = v1.getCreated().getTimeInMillis();
                    final Long t2 = v2.getCreated().getTimeInMillis();
                    return t1.compareTo(t2);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return versions;
    }

    private void createMigrationWorkspaceIfNotExists() throws RepositoryException {
        RepositoryImpl repositoryImpl = (RepositoryImpl) RepositoryDecorator.unwrap(defaultSession.getRepository());
        try {
            repositoryImpl.getRootSession(HANDLE_MIGRATION_WORKSPACE);
        } catch (NoSuchWorkspaceException e) {
            final InputSource is = new InputSource(getClass().getResourceAsStream("handlemigration-workspace.xml"));
            ((JackrabbitWorkspace)repositoryImpl.getRootSession(null).getWorkspace()).createWorkspace(HANDLE_MIGRATION_WORKSPACE, is);
        }
    }

    private Session loginToMigrationWorkspace() throws RepositoryException {
        RepositoryImpl repositoryImpl = (RepositoryImpl) RepositoryDecorator.unwrap(defaultSession.getRepository());
        final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
        return DecoratorFactoryImpl.getSessionDecorator(repositoryImpl.getRootSession(HANDLE_MIGRATION_WORKSPACE).impersonate(credentials), credentials);
    }

    public void cancel() {
        cancelled = true;
    }
}
