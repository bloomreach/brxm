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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConcurrentChannelManagerAndHstManagerLoadTest extends AbstractTestConfigurations {

    private HstManager hstManager;
    private ChannelManager channelManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        ((HstManagerImpl)hstManager).setStaleConfigurationSupported(true);
        this.channelManager = getComponent(ChannelManager.class.getName());
    }

    @Test
    public void testHstManagerConcurrentSynchronousLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            for (int i = 0; i < 500; i++) {
                jobs.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return hstManager.getVirtualHosts();
                    }
                });
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = (VirtualHosts)future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    @Test
    public void testConcurrentHstManagerSynchronousAndAsynchronousLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            final Random random = new Random();
            for (int i = 0; i < 500; i++) {
                final boolean allowStale;
                if (random.nextInt(2) == 0) {
                    allowStale = true;
                } else {
                    allowStale = false;
                }
                jobs.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return hstManager.getVirtualHosts(allowStale);
                    }
                });
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = (VirtualHosts)future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }


    @Test
    public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            final Random random = new Random();
            for (int i = 0; i < 500; i++) {
                int rand = random.nextInt(3);
                final boolean allowStale;
                if (rand == 0) {
                    // synchronous getting of virtualhosts 
                    jobs.add(new Callable<Object>() {
                        @Override
                        public VirtualHosts call() throws Exception {
                            return hstManager.getVirtualHosts();
                        }
                    });
                } else if (rand == 1){
                    // Asynchronous getting of virtualhosts 
                    jobs.add(new Callable<Object>() {
                        @Override
                        public VirtualHosts call() throws Exception {
                            return hstManager.getVirtualHosts(true);
                        }
                    });
                } else {
                    jobs.add(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            return channelManager.getChannels();
                        }
                    });
                }
                
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts currentHost = null;
            VirtualHosts nextHost;
            Channel currentChannel = null;
            Channel nextChannel;
            
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                Object o = future.get();
                if (o instanceof VirtualHosts) {
                    nextHost = (VirtualHosts) o;
                    if (currentHost == null) {
                        currentHost = nextHost;
                        continue;
                    }
                    assertTrue(currentHost == nextHost);
                } else {
                    Map<String, Channel> channelMap = (Map<String, Channel>) o;
                    assertTrue("Expected only one channel configured in unit test data ",channelMap.size() == 1);
                    nextChannel = channelMap.values().iterator().next();
                    if (currentChannel == null) {
                        currentChannel = nextChannel;
                        continue;
                    }
                    assertTrue(currentChannel == nextChannel);
                }
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    @Test
    public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerWithConfigChanges() throws Exception {
        // todo
    }

    
    
    protected Collection<Future<Object>> executeAllJobs(final Collection<Callable<Object>> jobs, final int threads) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final List<Future<Object>> futures = executorService.invokeAll(jobs);
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

}
