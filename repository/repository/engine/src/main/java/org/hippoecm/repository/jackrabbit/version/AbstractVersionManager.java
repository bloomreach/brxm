/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.jackrabbit.version;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.core.version.VersionManager;

/**
 * Base implementation of the {@link VersionManager} interface.
 * <p/>
 * All read operations must aquire the read lock before reading, all write
 * operations must aquire the write lock.
 */
public interface AbstractVersionManager extends VersionManager {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    public InternalVersion getVersion(NodeId id) throws RepositoryException;
    public InternalVersionHistory getVersionHistory(NodeId id) throws RepositoryException;
    public boolean hasVersionHistory(NodeId id);
    public boolean hasVersion(NodeId id);
    public void acquireWriteLock();
    public void releaseWriteLock();
    public void acquireReadLock();
    public void releaseReadLock();
    public VersionHistoryInfo getVersionHistory(Session session, NodeState node) throws RepositoryException;
    public InternalVersionItem getItem(NodeId id) throws RepositoryException;
    public boolean hasItem(NodeId id);
    public boolean hasItemReferences(InternalVersionItem item) throws RepositoryException;
    public InternalVersion checkin(InternalVersionHistoryImpl history, NodeImpl node) throws RepositoryException;
    public String calculateCheckinVersionName(InternalVersionHistoryImpl history, NodeImpl node) throws RepositoryException;
    public void removeVersion(InternalVersionHistoryImpl history, Name name) throws VersionException, RepositoryException;
    public InternalVersion setVersionLabel(InternalVersionHistoryImpl history, Name version, Name label, boolean move) throws RepositoryException;
    public void versionCreated(InternalVersion version);
    public void versionDestroyed(InternalVersion version);
    public void itemDiscarded(InternalVersionItem item);
    public InternalVersionItem createInternalVersionItem(NodeId id) throws RepositoryException;
}
