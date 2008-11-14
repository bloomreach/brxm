/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit.ver;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.VersionManager;

/**
 * Base implementation of the {@link VersionManager} interface.
 * <p/>
 * All read operations must aquire the read lock before reading, all write
 * operations must aquire the write lock.
 */
interface AbstractVersionManager extends VersionManager {

    public InternalVersion getVersion(NodeId id) throws RepositoryException;
    public InternalVersionHistory getVersionHistory(NodeId id) throws RepositoryException;
    public boolean hasVersionHistory(NodeId id);
    public boolean hasVersion(NodeId id);
    public void acquireWriteLock();
    public void releaseWriteLock();
    public void acquireReadLock();
    public void releaseReadLock();
    public VersionHistory getVersionHistory(Session session, NodeState node) throws RepositoryException;
    public InternalVersionItem getItem(NodeId id) throws RepositoryException;
    public boolean hasItem(NodeId id);
    public boolean hasItemReferences(InternalVersionItem item) throws RepositoryException;
    public InternalVersionHistory createVersionHistory(NodeState node) throws RepositoryException;
    public InternalVersion checkin(InternalVersionHistoryImpl history, NodeImpl node) throws RepositoryException;
    public String calculateCheckinVersionName(InternalVersionHistoryImpl history, NodeImpl node) throws RepositoryException;
    public void removeVersion(InternalVersionHistoryImpl history, Name name) throws VersionException, RepositoryException;
    public InternalVersion setVersionLabel(InternalVersionHistoryImpl history, Name version, Name label, boolean move) throws RepositoryException;
    public void versionCreated(InternalVersion version);
    public void versionDestroyed(InternalVersion version);
    public void itemDiscarded(InternalVersionItem item);
    public InternalVersionItem createInternalVersionItem(NodeId id) throws RepositoryException;
}
