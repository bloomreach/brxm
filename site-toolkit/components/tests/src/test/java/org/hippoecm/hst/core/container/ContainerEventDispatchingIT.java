/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.EventObject;

import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerEventDispatchingIT extends AbstractTestConfigurations {

    public class TestListener {

        private final Thread testThread;

        public TestListener(final Thread thread) {
            this.testThread = thread;
        }

        @Subscribe
        public void handleEvent(TestEventObject o) throws InterruptedException {
            Thread.sleep(o.sleep);
            o.setProcessed();
            o.setProcessedByThread(Thread.currentThread());
        }

        @Subscribe
        public void handleEventException(TestEventExceptionObject o) {
            throw o.exception;
        }

    }

    public class TestEventObject extends EventObject {

        private final long sleep;
        private boolean processed;
        private Thread processedByThread;

        public TestEventObject(final Object source, final long sleep) {
            super(source);
            this.sleep = sleep;
        }

        void setProcessed() {
            processed = true;
        }

        boolean isProcessed() {
            return processed;
        }

        public void setProcessedByThread(final Thread processedByThread) {
            this.processedByThread = processedByThread;
        }

        public Thread getProcessedByThread() {
            return processedByThread;
        }
    }

    private TestListener testListener;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testListener = new TestListener(Thread.currentThread());
        componentManager.registerEventSubscriber(testListener);
    }

    @After
    public void tearDown() throws Exception {
        componentManager.unregisterEventSubscriber(testListener);
        super.tearDown();
    }


    @Test
    public void assert_events_are_dispatched_synchronously() throws Exception {
        TestEventObject event = new TestEventObject(new Object(), 100L);
        componentManager.publishEvent(event);
        assertTrue("Event should have been processed even though it sleeps 100 ms because it should be handled synchronous.",
                event.isProcessed());
    }

    @Test
    public void assert_listeners_are_executed_with_same_thread_as_publisher() throws Exception {
        TestEventObject event = new TestEventObject(new Object(), 100L);
        componentManager.publishEvent(event);
        assertTrue("Event should have been processed by same thread as the thread that executes this test.",
                event.getProcessedByThread() == testListener.testThread);
    }

    public class TestEventExceptionObject extends EventObject {

        private final IllegalStateException exception;

        public TestEventExceptionObject(final Object o, final IllegalStateException e) {
            super(o);
            exception = e;
        }

    }

    @Test
    public void assert_listeners_exceptions_are_not_rethrown() throws Exception {
        TestEventExceptionObject eventException = new TestEventExceptionObject(new Object(), new IllegalStateException());
        try {
            componentManager.publishEvent(eventException);
        } catch (Exception e) {
            fail("Guava event bus did not rethrow exceptions while we implemented it");
        }


    }

}
