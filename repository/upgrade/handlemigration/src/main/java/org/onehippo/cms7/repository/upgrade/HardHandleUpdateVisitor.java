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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.version.VersionHistoryRemover;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoVersionManager;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeInfo;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.xml.sax.InputSource;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HARDDOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HARDHANDLE;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class HardHandleUpdateVisitor extends BaseContentUpdateVisitor {

    private static final String HANDLE_MIGRATION_WORKSPACE = "handlemigration";
    private static final String JCR_FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";
    private static final String ATTIC_PATH = "/content/attic";

    private static final int VERSIONS_RETAIN_COUNT = Integer.getInteger("org.onehippo.cms7.migration.versions_retain_count", Integer.MAX_VALUE);

    private Session defaultSession;
    private Session migrationSession;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        super.initialize(session);
        this.defaultSession = session;
        createMigrationWorkspaceIfNotExists();
        migrationSession = loginToMigrationWorkspace();
    }

    @Override
    public void destroy() {
        if (migrationSession != null && migrationSession.isLive()) {
            migrationSession.logout();
        }
    }

    @Override
    public boolean doUpdate(Node handle) throws RepositoryException {
        final String identifier = handle.getIdentifier();
        if (handle.getSession() != defaultSession) {
            handle = defaultSession.getNodeByIdentifier(identifier);
        }
        if (!handle.isNodeType(NT_HARDHANDLE)) {
            return false;
        }
        try {
            if (createTemporaryNode(identifier)) {
                try {
                    final VersionHistory handleVersionHistory = getHandleVersionHistory(handle);
                    final List<Version> versions = getDocumentVersions(handleVersionHistory);
                    if (!versions.isEmpty()) {
                        migrateVersionHistory(handle, versions);
                    }
                    migrateHandle(handle);
                    cleanVersionHistory(handleVersionHistory);
                } finally {
                    removeTemporaryNode(identifier);
                }
                return true;
            } else {
                return false;
            }
        } finally {
            defaultSession.refresh(false);
            migrationSession.refresh(false);
        }
    }

    private void migrateHandle(final Node handle) throws RepositoryException {
        log.debug("Migrating handle {}", handle.getPath());
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (variant.isNodeType(NT_HARDDOCUMENT)) {
                removeMixin(variant, NT_HARDDOCUMENT);
                if (!isUnpublishedVariant(variant) && variant.isNodeType(MIX_VERSIONABLE)) {
                    removeMixin(variant, MIX_VERSIONABLE);
                }
            }
        }
        removeMixin(handle, NT_HARDHANDLE);
        defaultSession.save();
        // have to do the following after removal of harddocument (getting exception deep down in jr)
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (isUnpublishedVariant(variant) && !variant.isNodeType(MIX_VERSIONABLE)) {
                variant.addMixin(MIX_VERSIONABLE);
                defaultSession.save();
            }
        }
    }

    private boolean isUnpublishedVariant(final Node variant) throws RepositoryException {
        return UNPUBLISHED.equals(JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null));
    }

    private void migrateVersionHistory(final Node handle, final List<Version> versions) throws RepositoryException {
        log.debug("Migrating version history of {}", handle.getPath());
        final String identifier = handle.getIdentifier();
        Node tmp = migrationSession.getNode("/" + identifier);
        replayHistory(tmp, versions);
        setPreview(handle, tmp.getIdentifier());
    }

    private void setPreview(final Node handle, final String identifier) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(handle);
        final String docPath = handle.getPath() + "/" + handle.getName();
        defaultSession.getWorkspace().clone(HANDLE_MIGRATION_WORKSPACE, "/" + handle.getIdentifier(), docPath, true);
        final Node newPreview = defaultSession.getNodeByIdentifier(identifier);
        boolean deleted = handle.getPath().startsWith(ATTIC_PATH);
        if (deleted) {
            clear(newPreview);
            newPreview.setPrimaryType(HippoNodeType.NT_DELETED);
            defaultSession.save();
        } else {
            final Node oldPreview = getOldPreview(handle, identifier);
            if (oldPreview != null) {
                copy(oldPreview, newPreview);
                oldPreview.remove();
            } else {
                setHippoAvailability(handle, newPreview);
            }
            defaultSession.save();
        }
    }

    private void setHippoAvailability(final Node handle, final Node newPreview) throws RepositoryException {
        newPreview.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});
        for (Node sibling : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (sibling.isSame(newPreview)) {
                continue;
            }
            if (sibling.hasProperty(HIPPO_AVAILABILITY)) {
                Value[] values = sibling.getProperty(HIPPO_AVAILABILITY).getValues();
                List<String> asList = new ArrayList<>();
                boolean modified = false;
                for (Value value : values) {
                    if ("preview".equals(value.getString())) {
                        modified = true;
                        continue;
                    }
                    asList.add(value.getString());
                }
                if (modified) {
                    JcrUtils.ensureIsCheckedOut(sibling);
                    sibling.setProperty(HIPPO_AVAILABILITY, asList.toArray(new String[asList.size()]), PropertyType.STRING);
                }
            }
        }
    }

    private Node getOldPreview(final Node handle, final String identifier) throws RepositoryException {
        for (Node document : new NodeIterable(handle.getNodes(handle.getName()))) {
            final String state = JcrUtils.getStringProperty(document, HIPPOSTD_STATE, null);
            if (UNPUBLISHED.equals(state) && !document.getIdentifier().equals(identifier)) {
                return document;
            }
        }
        return null;
    }

    private void replayHistory(final Node tmp, final List<Version> versions) throws RepositoryException {
        final HippoVersionManager versionManager = (HippoVersionManager)
                migrationSession.getWorkspace().getVersionManager();
        int count = 0;
        while (versions.size() > VERSIONS_RETAIN_COUNT) {
            versions.remove(0);
        }
        String tmpPath = tmp.getPath();
        for (Version version : versions) {
            copy(version.getFrozenNode(), tmp);
            tmp.setProperty(HIPPOSTD_STATE, UNPUBLISHED);
            migrationSession.save();
            versionManager.checkin(tmpPath, version.getCreated());
            versionManager.checkout(tmpPath);
            count++;
        }
        log.debug("Replayed {} versions", count);
    }

    private boolean createTemporaryNode(final String identifier) throws RepositoryException {
        final Node tmp = migrationSession.getRootNode().addNode(identifier);
        if (tmp.getIndex() == 1) {
            tmp.addMixin(MIX_VERSIONABLE);
            migrationSession.save();
            return true;
        } else {
            tmp.remove();
            return false;
        }
    }

    private void removeTemporaryNode(String identifier) {
        try {
            migrationSession.getNode("/" + identifier).remove();
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
        for (NodeType nodeType : srcNode.getMixinNodeTypes()) {
            if (!nodeType.getName().equals(MIX_VERSIONABLE)) {
                srcNode.removeMixin(nodeType.getName());
            }
        }
    }

    private void copy(final Node srcNode, final Node destNode) throws RepositoryException {
        JcrUtils.copyTo(srcNode, new OverwritingCopyHandler(destNode) {

            protected void replaceMixins(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
                String[] oldMixins = nodeInfo.getMixinNames();
                Set<String> mixins = new HashSet<>();
                for (String mixin : oldMixins) {
                    if (!NT_HARDDOCUMENT.equals(mixin)) {
                        mixins.add(mixin);
                    } else {
                        mixins.add(MIX_VERSIONABLE);
                    }
                }
                String[] newMixins = mixins.toArray(new String[mixins.size()]);
                NodeInfo newInfo = new NodeInfo(nodeInfo.getName(), nodeInfo.getIndex(), nodeInfo.getNodeTypeName(), newMixins);
                super.replaceMixins(node, newInfo);
            }

            @Override
            public void setProperty(final PropInfo prop) throws RepositoryException {
                final String name = prop.getName();
                if (name.startsWith("jcr:frozen") || name.startsWith("jcr:uuid") ||
                        name.equals(HippoNodeType.HIPPO_RELATED) ||
                        name.equals(HippoNodeType.HIPPO_COMPUTE) ||
                        name.equals(HippoNodeType.HIPPO_PATHS)) {
                    return;
                }
                super.setProperty(prop);
            }
        });

    }

    private void cleanVersionHistory(final VersionHistory handleVersionHistory) throws RepositoryException {
        final VersionIterator allHandleVersions = handleVersionHistory.getAllVersions();
        while (allHandleVersions.hasNext()) {
            final Version handleVersion = allHandleVersions.nextVersion();
            if (!handleVersion.getName().equals("jcr:rootVersion")) {
                final NodeIterator nodes = handleVersion.getNode("jcr:frozenNode").getNodes();
                while (nodes.hasNext()) {
                    final Node node = nodes.nextNode();
                    if (node.isNodeType("nt:versionedChild")) {
                        final String reference = node.getProperty("jcr:childVersionHistory").getString();
                        try {
                            final VersionHistory documentHistory = (VersionHistory) defaultSession.getNodeByIdentifier(reference);
                            VersionHistoryRemover.removeVersionHistory(documentHistory);
                        } catch (ItemNotFoundException ignore) {
                            // this can happen for items in the attic
                        }
                    }
                }
            }
        }
        VersionHistoryRemover.removeVersionHistory(handleVersionHistory);
    }

    private VersionHistory getHandleVersionHistory(final Node handle) throws RepositoryException {
        final VersionManager versionManager = defaultSession.getWorkspace().getVersionManager();
        return versionManager.getVersionHistory(handle.getPath());
    }

    private List<Version> getDocumentVersions(final VersionHistory handleVersionHistory) throws RepositoryException {
        final List<Version> versions = new ArrayList<Version>();

        final VersionIterator allHandleVersions = handleVersionHistory.getAllVersions();
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

}
