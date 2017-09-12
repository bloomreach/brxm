package org.onehippo.services.lock;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.onehippo.cms7.services.lock.LockManager;

public class LockManagerFactory {

    private RepositoryImpl repositoryImpl;

    public LockManagerFactory(final RepositoryImpl repositoryImpl) {
        this.repositoryImpl = repositoryImpl;
    }

    /**
     * Creates the {@link LockManager} which can be used for general purpose locking *not* using JCR at all
     *
     * @throws RuntimeException    if the lock manager cannot be created, resulting the repository startup to
     *                             short-circuit
     * @throws RepositoryException if a repository exception happened while creating the lock manager
     */
    public LockManager create() throws RuntimeException, RepositoryException {
        return new MemoryLockManager();
    }
}
