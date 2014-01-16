/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

public class LockAction extends Action {

    public LockAction(final ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(final Node node) throws Exception {
        return !node.isLocked();
    }

    @Override
    public boolean isWriteAction() {
        return true;
    }

    @Override
    protected Node doExecute(final Node node) throws Exception {
        ensureIsLockable(node);
        try {
            lock(node);
        } catch (LockException ignore) {}
        return node;
    }

    private void lock(final Node node) throws RepositoryException {
        final LockManager lockManager = node.getSession().getWorkspace().getLockManager();
        if (!lockManager.isLocked(node.getPath())) {
            lockManager.lock(node.getPath(), false, false, 1, null);
        }
    }

    private void ensureIsLockable(final Node node) throws RepositoryException {
        if (!node.isNodeType("mix:lockable")) {
            node.addMixin("mix:lockable");
            node.getSession().save();
        }
    }

}
