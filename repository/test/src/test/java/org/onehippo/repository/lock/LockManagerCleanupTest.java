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

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.repository.lock.db.DbLockCleanupJanitor;

public class LockManagerCleanupTest extends AbstractLockManagerTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final InternalLockManager internalLockManager = (InternalLockManager)HippoServiceRegistry.getService(LockManager.class);
        // Add a DbLockCleanupJanitor that runs evcery 5 secs instead of one per day to trigger it more frequently in the
        // integration tests below
        internalLockManager.addJob(new DbLockCleanupJanitor(dataSource), 0, 5);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        // since we add a LockManager job we need to tear down the repository : Otherwise, other integration tests
        // have this extra job also running
        super.tearDown(true);
    }

    @Test
    public void locks_with_lastmodified_older_than_a_day_get_cleaned_even_when_status_running() throws Exception {
        if (dataSource == null) {
            return;
        }
        // reason why status running still gets cleaned is because even a running lock should get its lastmodified updated
        long now = System.currentTimeMillis();
        long dayAgoTime = now - TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
        long halfDayAgoTime = now - TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);

        addManualLockToDatabase("1", "otherClusterNode", "threadName", dayAgoTime, now + 60_000, dayAgoTime);
        addManualLockToDatabase("2", getClusterNodeId(session), "threadName", dayAgoTime, now + 60_000,dayAgoTime);


        addManualLockToDatabase("3", "otherClusterNode", "threadName", dayAgoTime, now + 60_000,halfDayAgoTime);
        addManualLockToDatabase("4", getClusterNodeId(session), "threadName", dayAgoTime, now + 60_000,halfDayAgoTime);

        addManualLockToDatabase("5", "otherClusterNode", "threadName", dayAgoTime, now + 60_000,now);
        addManualLockToDatabase("6", getClusterNodeId(session), "threadName", dayAgoTime, now + 60_000,now);

        Thread.sleep(10_000);

        // d,e,f,g should still be there because last modified not a day ago
        dbRowAssertion("3", "RUNNING");
        dbRowAssertion("4", "RUNNING");
        dbRowAssertion("5", "RUNNING");
        dbRowAssertion("6", "RUNNING");

        // a, b, have lastmodified older than a day and should had been removed
        assertKeyMissing("1");
        assertKeyMissing("2");
    }


}
