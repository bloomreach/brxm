/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.lock;


public class Lock {

    private final String lockKey;

    private final String lockOwner;

    private final String lockThread;

    private final long lockTime;

    private final String status;

    public Lock(final String lockKey, final String lockOwner, final String lockThread, final long lockTime,
                final String status) {
        this.lockKey = lockKey;
        this.lockOwner = lockOwner;
        this.lockThread = lockThread;
        this.lockTime = lockTime;
        this.status = status;
    }

    public String getLockKey() {
        return lockKey;
    }

    /**
     * @return the cluster node id and in case of no clustering, it will be 'default'
     */
    public String getLockOwner() {
        return lockOwner;
    }

    /**
     * @return the name of the thread that holds the lock
     */
    public String getLockThread() {
        return lockThread;
    }

    public long getLockTime() {
        return lockTime;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Lock{" +
                "lockKey='" + lockKey + '\'' +
                ", lockOwner='" + lockOwner + '\'' +
                ", lockThread='" + lockThread + '\'' +
                ", lockTime=" + lockTime +
                ", status='" + status + '\'' +
                '}';
    }
}
