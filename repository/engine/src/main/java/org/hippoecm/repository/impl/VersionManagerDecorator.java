/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.VersionManagerImpl;
import org.hippoecm.repository.api.HippoVersionManager;

public class VersionManagerDecorator implements HippoVersionManager {

    private final VersionManager versionManager;
    private final WorkspaceDecorator workspace;
    private final SessionDecorator session;

    public static VersionManager unwrap(final VersionManager versionManager) {
        if (versionManager instanceof VersionManagerDecorator) {
            return ((VersionManagerDecorator)versionManager).versionManager;
        }
        return versionManager;
    }

    public VersionManagerDecorator(final VersionManager versionManager, final WorkspaceDecorator workspaceDecorator) {
        this.versionManager = unwrap(versionManager);
        this.workspace = workspaceDecorator;
        this.session = workspace.session;
    }

    @Override
    public VersionDecorator checkin(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return new VersionDecorator(session, versionManager.checkin(absPath));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void checkout(final String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.checkout(absPath);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public VersionDecorator checkpoint(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return new VersionDecorator(session, versionManager.checkpoint(absPath));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public boolean isCheckedOut(final String absPath) throws RepositoryException {
        return versionManager.isCheckedOut(absPath);
    }

    @Override
    public VersionHistoryDecorator getVersionHistory(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return new VersionHistoryDecorator(session, versionManager.getVersionHistory(absPath));
    }

    @Override
    public VersionDecorator getBaseVersion(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return new VersionDecorator(session, versionManager.getBaseVersion(absPath));
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        Version[] unwrapped = Arrays.stream(versions).map(v -> VersionDecorator.unwrap(v)).toArray(size -> new Version[size]);
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(unwrapped, removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void restore(final String absPath, final String versionName, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(absPath, versionName, removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(VersionDecorator.unwrap(version), removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void restore(final String absPath, final Version version, final boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(absPath, VersionDecorator.unwrap(version), removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void restoreByLabel(final String absPath, final String versionLabel, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restoreByLabel(absPath, versionLabel, removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public NodeIteratorDecorator merge(final String absPath, final String srcWorkspace, final boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return new NodeIteratorDecorator(session, versionManager.merge(absPath, srcWorkspace, bestEffort));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public NodeIteratorDecorator merge(final String absPath, final String srcWorkspace, final boolean bestEffort, final boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return new NodeIteratorDecorator(session, versionManager.merge(absPath, srcWorkspace, bestEffort, isShallow));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void doneMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.doneMerge(absPath, VersionDecorator.unwrap(version));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void cancelMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.cancelMerge(absPath, VersionDecorator.unwrap(version));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public NodeDecorator createConfiguration(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return NodeDecorator.newNodeDecorator(session, versionManager.createConfiguration(absPath));
    }

    @Override
    public NodeDecorator setActivity(final Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        return NodeDecorator.newNodeDecorator(session, versionManager.setActivity(NodeDecorator.unwrap(activity)));
    }

    @Override
    public NodeDecorator getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        return NodeDecorator.newNodeDecorator(session, versionManager.getActivity());
    }

    @Override
    public NodeDecorator createActivity(final String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        return NodeDecorator.newNodeDecorator(session, versionManager.createActivity(title));
    }

    @Override
    public void removeActivity(final Node activityNode) throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        versionManager.removeActivity(NodeDecorator.unwrap(activityNode));
    }

    @Override
    public NodeIteratorDecorator merge(final Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return new NodeIteratorDecorator(session, versionManager.merge(NodeDecorator.unwrap(activityNode)));
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public VersionDecorator checkin(final String absPath, final Calendar created) throws RepositoryException {
        return new VersionDecorator(session, ((VersionManagerImpl) versionManager).checkin(absPath, created));
    }
}
