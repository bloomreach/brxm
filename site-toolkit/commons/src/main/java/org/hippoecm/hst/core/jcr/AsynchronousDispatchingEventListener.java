/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class can be used if the events need to be processed asynchronous.
 * Hippo Repository used to create synchronous observers, but it creates asynchronous observers since 2.23.05 by default.
 * If you depend on a synchronous observer in a synchronized method and at the same time read nodes 
 * in another synchronized method, then in clustered environments deadlocks can occur.
 * 
 * A way to circumvent this, is to extend from this {@link AsynchronousDispatchingEventListener} and implement 
 * {@link #onAsynchronousEvent(EventIterator)} yourself : This method is invoked with a different Thread and with a jcr {@link EventIterator} which 
 * is detached from the repository containing {@link Event}s which are detached from the repository
 * 
 * @deprecated Hippo Repository creates asynchronous observer by default since 2.25.05. Your custom eventListener implementation
 * can now just extend {@link GenericEventListener} directly and rename {@link #onAsynchronousEvent(javax.jcr.observation.EventIterator)} to {@link #onEvent(javax.jcr.observation.EventIterator)}. Do not extend from this {@link AsynchronousDispatchingEventListener} any more
 * @see GenericEventListener
 */
@Deprecated
public abstract class AsynchronousDispatchingEventListener extends GenericEventListener {
    
    static Logger log = LoggerFactory.getLogger(AsynchronousDispatchingEventListener.class);

    /**
     * The service that will execute the {@link AsynchronousEventDispatcher}
     */
    private ExecutorService executor;
    
    /**
     * Creates a AsynchronousDispatchingEventListener where the ExecutorService is a single threaded executor
     */
    public AsynchronousDispatchingEventListener() {
        log.warn("{} extends from AsynchronousDispatchingEventListener which has been deprecated. Extend from GenericEventListener instead", getClass().getName());
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Creates a AsynchronousDispatchingEventListener with ExecutorService <code>executor</code>
     * @param executor the ExecutorService
     */
    public AsynchronousDispatchingEventListener(ExecutorService executor) {
        this.executor = executor;
    }
    
    /**
     * This method is called when a bundle of events is dispatched asynchronously and detached from the repository listener.
     *
     * @param events The event set received.
     */
    abstract public void onAsynchronousEvent(EventIterator events);
    
    @Override
    final public void onEvent(EventIterator events) {
        List<Event> detachedEventList = new ArrayList<Event>();
        while (events.hasNext()) {
            Event event = events.nextEvent();
            String path = null;
            try {
                path = event.getPath();
                Event detachedEvent = new DetachedEvent(event);
                detachedEventList.add(detachedEvent);
            } catch (RepositoryException e) {
                if(path == null) {
                    HstServices.getLogger(getClass().getName()).warn("Repository exception during processing event. Could not getPath() for the event. Processing next event", e);
                } else {
                    HstServices.getLogger(getClass().getName()).warn("Repository exception during processing event with path '"+path+"'. Processing next event", e);
                }
                continue;
            }
        } 
        DetachedEventIterator iterator = new DetachedEventIterator(detachedEventList.toArray(new Event[detachedEventList.size()]));
        
        executor.execute(new AsynchronousEventDispatcher(iterator, this));
       
    }
    
    /**
     * A Runnable that when run is called dispatches the events to the {@link AsynchronousDispatchingEventListener}
     */
    private class AsynchronousEventDispatcher implements Runnable {
        
        EventIterator events;
        AsynchronousDispatchingEventListener listener;
        
        private AsynchronousEventDispatcher(EventIterator events, AsynchronousDispatchingEventListener listener) {
            this.events = events;
            this.listener = listener;
        }
        
        @Override
        public void run() {
            listener.onAsynchronousEvent(events);
        }
    }
    
    /**
     * We need an {@link EventIterator} which is completely detached from the {@link Repository}. The repository {@link EventIterator} can hold for example
     * {@link Event}s which in turn hold {@link Session} or NodeState which we do not want in our asynchronous dispatching
     */
    private class DetachedEventIterator implements EventIterator {

        /** The current iterator position. */
        private int position;

        /** The underlying array of events. */
        final private Event[] events;

        /**
         * Creates an iterator for the given array of objects.
         *
         * @param events the Events to iterate
         */
        public DetachedEventIterator(Event[] events) {
            this.position = 0;
            this.events = events;
        }

        @Override
        public boolean hasNext() {
            return (position < events.length);
        }

        @Override
        public Event next() {
            return events[position++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void skip(long items) {
            position += items;
            if (position > events.length) {
                position = events.length;
                throw new NoSuchElementException(
                        "Skipped past the last element of the iterator");
            }
        }

        @Override
        public long getSize() {
            return events.length;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public Event nextEvent() {
            return next();
        }
    }
    
    
    /**
     * We need an {@link Event} which is completely detached from the {@link Repository}. The repository {@link Event} can hold for example
     * a {@link Session} or NodeState which we do not want in our asynchronous dispatching
     */
    private class DetachedEvent implements Event {
        final long date; 
        final String identifier; 
        final String path;
        final int type;
        final String userData;
        final String userID; 
        
        // NEVER STORE THE EVENT : IT CONTAINS THE JCR SESSION. 
        // only fetch the serializable data
        public DetachedEvent(Event event) throws RepositoryException {
            date = event.getDate();
            identifier = event.getIdentifier();
            path = event.getPath();
            type = event.getType();
            userData = event.getUserData();
            userID = event.getUserID();
        }

        @Override
        public long getDate() throws RepositoryException {
            return date;
        }

        @Override
        public String getIdentifier() throws RepositoryException {
            return identifier;
        }

        @Override
        public Map getInfo() throws RepositoryException {
            throw new UnsupportedOperationException("getInfo not supported on RepoDetachedEvent");
        }

        @Override
        public String getPath() throws RepositoryException {
            return path;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public String getUserData() throws RepositoryException {
            return userData;
        }

        @Override
        public String getUserID() {
            return userID;
        }
        
    }
    
    
}
