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
package org.hippoecm.repository.impl;

import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.VersionManagerImpl;
import org.hippoecm.repository.api.HippoVersionManager;


public class VersionManagerDecorator implements HippoVersionManager {

    private final VersionManager versionManager;
    private final WorkspaceDecorator workspace;

    public VersionManagerDecorator(final VersionManager versionManager, final WorkspaceDecorator workspaceDecorator) {
        this.versionManager = versionManager;
        this.workspace = workspaceDecorator;
    }

    @Override
    public Version checkin(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return versionManager.checkin(absPath);
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
    public Version checkpoint(final String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return versionManager.checkpoint(absPath);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public boolean isCheckedOut(final String absPath) throws RepositoryException {
        return versionManager.isCheckedOut(absPath);
    }

    @Override
    public VersionHistory getVersionHistory(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.getVersionHistory(absPath);
    }

    @Override
    public Version getBaseVersion(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.getBaseVersion(absPath);
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(versions, removeExisting);
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
            versionManager.restore(version, removeExisting);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void restore(final String absPath, final Version version, final boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.restore(absPath, version, removeExisting);
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
    public NodeIterator merge(final String absPath, final String srcWorkspace, final boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return versionManager.merge(absPath, srcWorkspace, bestEffort);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public NodeIterator merge(final String absPath, final String srcWorkspace, final boolean bestEffort, final boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return versionManager.merge(absPath, srcWorkspace, bestEffort, isShallow);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void doneMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.doneMerge(absPath, version);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public void cancelMerge(final String absPath, final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            versionManager.cancelMerge(absPath, version);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public Node createConfiguration(final String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.createConfiguration(absPath);
    }

    @Override
    public Node setActivity(final Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.setActivity(activity);
    }

    @Override
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.getActivity();
    }

    @Override
    public Node createActivity(final String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager.createActivity(title);
    }

    @Override
    public void removeActivity(final Node activityNode) throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        versionManager.removeActivity(activityNode);
    }

    @Override
    public NodeIterator merge(final Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            workspace.postMountEnabled(false);
            return versionManager.merge(activityNode);
        } finally {
            workspace.postMountEnabled(true);
        }
    }

    @Override
    public Version checkin(final String absPath, final Calendar created) throws RepositoryException {
        return ((VersionManagerImpl) versionManager).checkin(absPath, created);
    }
}
