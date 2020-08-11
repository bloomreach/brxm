/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.lock;

import org.junit.Test;

public class LockManagerExpiresTest extends AbstractLockManagerTest {


    @Test
    public void an_expired_lock_gets_freed() throws Exception {
        if (dbLockManager == null) {
            // in memory test
            return;
        }
        long expirationTime = System.currentTimeMillis() + 6000;
        insertDataRowLock("123", "otherClusterNode", "otherThread", expirationTime);

        // Since the lock is of another cluster node which is not live, the lock does not get refreshed meaning it
        // will be freed after expiration time

        // within 15 seconds the lock should for sure be freed
        while (System.currentTimeMillis() < expirationTime + 15000) {
            try {
                dbRowAssertion("123", "FREE");
                break;
            } catch (AssertionError e) {
                // status not yet free, retry
            }
        }

        // the lock should be free
        dbRowAssertion("123", "FREE");
    }


}
