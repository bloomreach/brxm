/*
 *  Copyright 2011 Hippo.
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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * This abstract class can be used if the events need to be processed asynchronous. By default, hippo repository creates 
 * synchronous observers. If you depend on a synchronous observer in a synchronized method and at the same time read nodes 
 * in another synchronized method, then in clustered environments deadlocks can occur.
 * 
 * A way to circumvent this, is to extend from this {@link AsynchronousDispatchingEventListener} and implement 
 * {@link #onAsynchronousEvent(EventIterator)} yourself : This method is invoked with a different Thread and with a jcr {@link EventIterator} which 
 * is detached from the repository containing {@link Event}s which are detached from the repository
 */
public abstract class AsynchronousDispatchingEventListener extends GenericEventListener {
   
    /**
     * This method is called when a bundle of events is dispatched asynchronously from the repository listener.
     *
     * @param events The event set received.
     */
    abstract public void onAsynchronousEvent(EventIterator events);
    
    @Override
    final public void onEvent(EventIterator events) {
        List<Event> detachedEventList = new ArrayList<Event>();
        while (events.hasNext()) {
            Event event = events.nextEvent();
            try {
                Event detachedEvent = new DetachedEvent(event);
                detachedEventList.add(detachedEvent);
            } catch (RepositoryException e) {
                continue;
            }
        }
        DetachedEventIterator iterator = new DetachedEventIterator(detachedEventList.toArray(new Event[detachedEventList.size()]));
        // start the asynchronous dispatching with another Thread
        new AsynchronousEventDispatcher(iterator, this).start();
       
    }
    
    /**
     * A dispatcher Thread that when run is called dispatches the events to the {@link AsynchronousDispatchingEventListener}
     */
    private class AsynchronousEventDispatcher extends Thread {
        
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
        private Event[] events;

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
        long date; 
        String identifier; 
        String path;
        int type;
        String userData;
        String userID; 
        
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
