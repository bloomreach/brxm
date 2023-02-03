/*
 *  Copyright 2018-2023 Bloomreach
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
package org.hippoecm.hst.platform.model;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstEventConsumer;
import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.configuration.cache.HstEventsDispatcher;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.model.HstConfigurationEventListener;
import org.hippoecm.repository.api.SynchronousEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PERSIST;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

public class InvalidationMonitor {

    private final static Logger log = LoggerFactory.getLogger(InvalidationMonitor.class.getName());
    private final Session session;
    private final Session syncSession;
    private final HstConfigurationEventListener hstConfigurationEventListener;
    private final SynchronousOnEventCounter synchronousOnEventCounter;
    private final HstEventsCollector hstEventsCollector;
    private final HstEventsDispatcher hstEventsDispatcher;

    public InvalidationMonitor(final Session session,
                               final HstNodeLoadingCache hstNodeLoadingCache,
                               final HstConfigurationLoadingCache hstConfigurationLoadingCache,
                               final HstModelImpl hstModelImpl) throws RepositoryException {

        this.session = session;

        syncSession = session.impersonate(new SimpleCredentials("configuser", "".toCharArray()));

        hstEventsCollector = new HstEventsCollector(hstNodeLoadingCache.getRootPath());

        // jcr listener to collect hst events in 'hstEventsCollector'
        hstConfigurationEventListener = new HstConfigurationEventListener(hstModelImpl, hstEventsCollector);

        synchronousOnEventCounter = new SynchronousOnEventCounter();

        session.getWorkspace().getObservationManager().addEventListener(hstConfigurationEventListener,
                getAnyEventType(), hstNodeLoadingCache.getRootPath(), true, null, null, false);

        syncSession.getWorkspace().getObservationManager().addEventListener(synchronousOnEventCounter,
                getAnyEventType(), hstNodeLoadingCache.getRootPath(), true, null, null, false);

        hstEventsDispatcher = new HstEventsDispatcher(hstEventsCollector, Arrays.asList(new HstEventConsumer[]{hstNodeLoadingCache, hstConfigurationLoadingCache}));
    }

    protected void dispatchHstEvents() {
        hstEventsDispatcher.dispatchHstEvents();
    }

    protected void destroy() throws RepositoryException {
        session.getWorkspace().getObservationManager().removeEventListener(hstConfigurationEventListener);
        // session gets logged out by HstModelImopl#destroy
        syncSession.getWorkspace().getObservationManager().removeEventListener(synchronousOnEventCounter);
        syncSession.logout();
    }

    public HstEventsCollector getHstEventsCollector() {
        return hstEventsCollector;
    }

    private int getAnyEventType() {
        return NODE_ADDED | NODE_MOVED | NODE_REMOVED | PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED | PERSIST;
    }


    public long getAsynchronousOnEventsCounter() {
        return hstConfigurationEventListener.getOnEventCount();
    }

    public long getSynchronousOnEventsCounter() {
        return synchronousOnEventCounter.getOnEventCount();
    }

    // likely there will arrive async JCR events invoking #invalidate(), let's wait a bit if that happens
    // because it is async and invalidationMonitor.getAsynchronousOnEventsCounter() updates synchronized on this
    // object, do not synchronize this part on (this) HstModelImpl
    public void awaitEventsConsistency() {

        // max wait 1 second for JCR events to arrive : heuristics of 1 second found during monitoring tests on
        // jenkins: during heavy operations with eg branch merging the delay can be significant
        long maxWait = 1000;

        final long start = System.currentTimeMillis();
        final long waitUntil = start + maxWait;

        final long synchronousOnEventsCounter = getSynchronousOnEventsCounter();
        final long asynchronousOnEventsCounter = getAsynchronousOnEventsCounter();

        // in the extreme unlikely situation, after getSynchronousOnEventsCounter() is invoked, the async event counter
        // could receive (just like the sync event counter) a new event, hence in the if below => instead of just =
        if (asynchronousOnEventsCounter >= synchronousOnEventsCounter) {
            log.debug("asynchronousOnEventsCounter is on par with synchronousOnEventsCounter");
            return;
        }

        int loop = 0;
        while (getAsynchronousOnEventsCounter() < synchronousOnEventsCounter) {
            if (System.currentTimeMillis() > waitUntil) {
                log.warn("Expected within {} ms the async HstConfiguration EventListener to have caught up with the " +
                                "InvalidationMonitor#SynchronousOnEventCounter listener counter. However async counter is as '{}' " +
                                "and sync counter at '{}'. Stop waiting longer.", maxWait, getAsynchronousOnEventsCounter(),
                        synchronousOnEventsCounter);
                break;
            }

            // only log every ~100 ms
            if (loop++ % 10 == 0) {
                log.debug("Waiting for async events to arrive. Async counter is at '{}'. Waiting until it is at least '{}'",
                        getAsynchronousOnEventsCounter(), synchronousOnEventsCounter);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.error("InterruptedException, proceed", e);
            }
        }
        log.info("asynchronousOnEventsCounter on par with synchronousOnEventsCounter within '{}' ms", System.currentTimeMillis() - start);
    }

    private class SynchronousOnEventCounter implements EventListener, SynchronousEventListener {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public void onEvent(final EventIterator events) {
            counter.incrementAndGet();
        }

        public long getOnEventCount() {
            return counter.get();
        }
    };
}
