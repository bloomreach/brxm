package org.hippoecm.repository.jackrabbit.version;

import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.VersionManager;

public interface HippoVersionManager extends VersionManager {

    public InternalVersionItem getItem(NodeId id)
            throws RepositoryException;

    public void acquireReadLock();

    public void releaseReadLock();

    public void acquireWriteLock();

    public void releaseWriteLock();

    public void versionCreated(InternalVersion version);

    public void versionDestroyed(InternalVersion version);

    public boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException;
}
