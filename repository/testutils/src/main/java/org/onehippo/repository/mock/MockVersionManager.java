/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.naming.OperationNotSupportedException;

import org.onehippo.repository.util.JcrConstants;

/**
 * Mock version of {@link javax.jcr.version.VersionManager}. Supports checking in and out of nodes, and basic
 * version retrieval.
 */
public class MockVersionManager implements VersionManager {

    private final MockSession session;
    private final MockNode versionStorage;

    MockVersionManager(final MockSession session) throws RepositoryException {
        this.session = session;

        MockNode root = session.getRootNode();
        this.versionStorage = getOrAddNode(root, "mockVersionStorage", "mock:versionStorage");
    }

    private static MockNode getOrAddNode(final MockNode parent, final String name, final String type) throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }
        return parent.addNode(name, type);
    }

    @Override
    public MockVersion checkin(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        MockNode node = session.getNode(absPath);
        if (!node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException("Node '" + absPath + "' is not versionable");
        }
        node.setProperty(JcrConstants.JCR_IS_CHECKED_OUT, false);
        node.setCheckedOut(false);
        MockVersionHistory history = getVersionHistory(absPath);
        return history.addVersion();
    }

    @Override
    public void checkout(final String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        MockNode node = session.getNode(absPath);
        if (!node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException("Node '" + absPath + "' is not versionable");
        }
        node.setCheckedOut(true);
        node.setProperty(JcrConstants.JCR_IS_CHECKED_OUT, true);
    }

    @Override
    public Version checkpoint(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut(final String absPath) throws RepositoryException {
        return session.getNode(absPath).isCheckedOut();
    }

    @Override
    public MockVersionHistory getVersionHistory(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        MockNode node = session.getNode(absPath);

        if (!node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException("Node " + node.getPath() + " is not versionable because it does not have the mixin '" + JcrConstants.MIX_VERSIONABLE + "'");
        }

        final String nodeId = node.getIdentifier();

        if (!versionStorage.hasNode(nodeId)) {
            MockVersionHistory history = new MockVersionHistory(node);
            versionStorage.addNode(history);
            return history;
        } else {
            return (MockVersionHistory) versionStorage.getNode(nodeId);
        }
    }

    @Override
    public Version getBaseVersion(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final String absPath, final String versionName, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final String absPath, final Version version, final boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreByLabel(final String absPath, final String versionLabel, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final String absPath, final String srcWorkspace, final boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final String absPath, final String srcWorkspace, final boolean bestEffort, final boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doneMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node createConfiguration(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node setActivity(final Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node createActivity(final String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeActivity(final Node activityNode) throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }
}
