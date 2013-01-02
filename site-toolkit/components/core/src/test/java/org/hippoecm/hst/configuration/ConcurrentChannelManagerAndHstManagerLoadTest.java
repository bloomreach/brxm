/*
 *  Copyright 2013 Hippo.
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
package org.hippoecm.hst.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConcurrentChannelManagerAndHstManagerLoadTest extends AbstractTestConfigurations {

    private HstManager hstManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
    }

    @Test
    public void testHstManagerConcurrentSynchronousLoad() throws Exception {
        try {
            Collection<Callable<VirtualHosts>> jobs = new ArrayList<Callable<VirtualHosts>>(500);
            for (int i = 0; i < 500; i++) {
                jobs.add(new SynchronousVirtualHostsFetcher());
            }
            final List<Future<VirtualHosts>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<VirtualHosts> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    @Test
    public void testConcurrentHstManagerSynchronousAndAsynchronousLoad() throws Exception {
        try {
            Collection<Callable<VirtualHosts>> jobs = new ArrayList<Callable<VirtualHosts>>(500);
            final Random random = new Random();
            for (int i = 0; i < 500; i++) {
                if (random.nextInt(2) == 0) {
                    jobs.add(new SynchronousVirtualHostsFetcher());
                } else {
                    jobs.add(new ASynchronousVirtualHostsFetcher());
                }
            }
            final List<Future<VirtualHosts>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<VirtualHosts> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (Throwable e) {
            fail(e.toString());
        }
    }


    @Test
    public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerLoad() throws Exception {
        // todo
    }


    @Test
    public void testConcurrentAsyncAndSyncLoadsDuringChanges() throws Exception {
        // todo
    }

    private List<Future<VirtualHosts>> executeAllJobs(final Collection<Callable<VirtualHosts>> jobs, final int threads) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final List<Future<VirtualHosts>> futures = executorService.invokeAll(jobs);
        executorService.shutdown();
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            executorService.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                fail("Pool did not terminate");
            }
        }
        return futures;
    }

    public class SynchronousVirtualHostsFetcher implements Callable<VirtualHosts> {
        @Override
        public VirtualHosts call() throws Exception {
            return hstManager.getVirtualHosts();
        }
    }

    public class ASynchronousVirtualHostsFetcher implements Callable<VirtualHosts> {
        @Override
        public VirtualHosts call() throws Exception {
            return hstManager.getVirtualHosts(true);
        }
    }
}
