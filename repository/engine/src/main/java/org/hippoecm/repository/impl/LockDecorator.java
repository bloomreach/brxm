/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

public class LockDecorator extends SessionBoundDecorator implements Lock {

    private Lock lock;

    public static Lock unwrap(final Lock lock) {
        if (lock instanceof LockDecorator) {
            return ((LockDecorator)lock).lock;
        }
        return lock;
    }

    LockDecorator(final SessionDecorator session, final Lock lock) {
        super(session);
        this.lock = unwrap(lock);
    }

    @Override
    public String getLockOwner() {
        return lock.getLockOwner();
    }

    @Override
    public boolean isDeep() {
        return lock.isDeep();
    }

    @Override
    public Node getNode() {
        return NodeDecorator.newNodeDecorator(session, lock.getNode());
    }

    @Override
    public String getLockToken() {
        return lock.getLockToken();
    }

    @Override
    public long getSecondsRemaining() throws RepositoryException {
        return lock.getSecondsRemaining();
    }

    @Override
    public boolean isLive() throws RepositoryException {
        return lock.isLive();
    }

    @Override
    public boolean isSessionScoped() {
        return lock.isSessionScoped();
    }

    @Override
    public boolean isLockOwningSession() {
        return lock.isLockOwningSession();
    }

    @Override
    public void refresh() throws LockException, RepositoryException {
        lock.refresh();
    }
}
