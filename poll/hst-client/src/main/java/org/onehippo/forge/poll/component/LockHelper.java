/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.poll.component;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LockHelper {
    public static final Logger logger = LoggerFactory.getLogger(LockHelper.class);

    private int timeoutMillis = 5000;
    private int sleepMillis = 250;

    public LockHelper() {
        super();
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void setSleepMillis(int sleepMillis) {
        this.sleepMillis = sleepMillis;
    }


    public boolean getLock(Node node) {

        if (!makeLockable(node)) {
            return false;
        }

        boolean isLocked = false;
        long timeout = this.timeoutMillis + System.currentTimeMillis();

        while (!isLocked && (System.currentTimeMillis() < timeout)) {
            try {
                String nodePath = node.getCorrespondingNodePath(node.getSession().getWorkspace().getName());
                isLocked = doGetLock(node, nodePath);

                if (!isLocked) {
                    Thread.sleep(this.sleepMillis);
                }
            }
            catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            catch (RepositoryException e) {
                logger.error(e.getMessage());
            }
        }

        return isLocked;
    }

    public void unlock(Node node) {
        try {
            Workspace workspace = node.getSession().getWorkspace();
            LockManager lockManager = workspace.getLockManager();
            String nodePath = node.getCorrespondingNodePath(workspace.getName());

            try {
                if (node.isLocked()) {
                    lockManager.unlock(nodePath);
                }
            }
            catch (LockException le) {
                // cannot unlock node? try to undo changes and force it
                try {
                    logger.error("unable to remove the lock for node {}, undoing all changes and trying to force remove this lock", nodePath);
                    node.refresh(false);
                    lockManager.unlock(nodePath);
                }
                catch (RepositoryException e) {
                   logger.error("RepositoryException: unable to remove the lock for node " + nodePath +  ", even after undoing changes. Origination exception stacktrace:", le);
                }
            }
            catch (RepositoryException e) {
                logger.error(e.getMessage());
            }
        }
        catch (RepositoryException e) {
            logger.error(e.getMessage());
        }
    }

    private boolean doGetLock(Node node, String nodePath) {
        try {
            if (!node.isLocked()) {
                LockManager lockManager = node.getSession().getWorkspace().getLockManager();
                lockManager.lock(nodePath, true /*isDeep*/, true /*isSessionScoped*/, 5 /*timeoutHint*/, "" /*ownerInfo*/);
                return true;
            }

        }
        catch (LockException e) {
            //someone beat us to the punch, next time better luck
        }
        catch (AccessDeniedException e) {
            try {
                logger.error("Lock access denied for user {} on node {}", node.getSession().getUserID(), node.getPath());
            }
            catch (RepositoryException e1) {
                logger.error(e.getMessage());
            }
        }
        catch (Exception e) {
            try {
                logger.error("Failed setting locked lock for {}, message is {}", node.getPath(), e.getMessage());
            }
            catch (RepositoryException e1) {
                logger.error("Failed setting locked lock, message is {}",  e1.getMessage());
            }
            throw new RuntimeException(e.getMessage());
         }
        return false;
    }

    private boolean makeLockable(Node node) {
        try {
            NodeType[] nodeTypes = node.getMixinNodeTypes();
            for (NodeType nodeType : nodeTypes) {
                if ("mix:lockable".equals(nodeType.getName())) {
                    return true;
                }
            }
            if (node.canAddMixin("mix:lockable")) {
                node.addMixin("mix:lockable");
                node.getSession().save();
                node.refresh(false);
                return true;
            }
        }
        catch (RepositoryException re) {
            logger.error(re.getMessage());
        }
        return false;
    }
}
