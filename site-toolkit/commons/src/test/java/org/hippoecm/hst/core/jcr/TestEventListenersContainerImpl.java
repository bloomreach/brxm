/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.makeThreadSafe;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TestEventListenersContainer
 * 
 * @version $Id$
 */
public class TestEventListenersContainerImpl {
    
    private Repository repository;
    private Session session;
    private Workspace workspace;
    private MockObservationManager observationManager;
    private Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
    
    @Before
    public void setUp() throws Exception {
        repository = createNiceMock(Repository.class);
        session = createNiceMock(Session.class);
        workspace = createNiceMock(Workspace.class);
        observationManager = new MockObservationManager();
        
        makeThreadSafe(repository, true);
        makeThreadSafe(session, true);
        makeThreadSafe(workspace, true);
        
        expect(repository.login(credentials)).andReturn(session).anyTimes();
        expect(session.isLive()).andReturn(true).anyTimes();
        expect(session.getWorkspace()).andReturn(workspace).anyTimes();
        expect(workspace.getObservationManager()).andReturn(observationManager).anyTimes();
        
        replay(repository);
        replay(session);
        replay(workspace);
    }
    
    @Test
    public void testSimpleLifecycle() throws Exception {
        List<EventListenerItem> eventListenerItems = new ArrayList<EventListenerItem>();
        
        eventListenerItems.add(createEventListenerItem("/hst:configurations"));
        
        EventListenersContainerImpl listenersContainer = new EventListenersContainerImpl();
        listenersContainer.setRepository(repository);
        listenersContainer.setCredentials(credentials);
        listenersContainer.setSessionLiveCheck(false);
        listenersContainer.setSessionLiveCheckInterval(3000L);
        listenersContainer.setSessionLiveCheckIntervalOnStartup(60000L);
        listenersContainer.setEventListenerItems(eventListenerItems);
        
        assertEquals(0, observationManager.size());
        listenersContainer.start();
        assertEquals(eventListenerItems.size(), observationManager.size());
        listenersContainer.stop();
        assertEquals(0, observationManager.size());
    }
    
    @Test
    public void testSessionCheckingLifecycle() throws Exception {
        List<EventListenerItem> eventListenerItems = new ArrayList<EventListenerItem>();
        
        eventListenerItems.add(createEventListenerItem("/hst:configurations"));
        
        EventListenersContainerImpl listenersContainer = new EventListenersContainerImpl();
        listenersContainer.setRepository(repository);
        listenersContainer.setCredentials(credentials);
        listenersContainer.setSessionLiveCheck(true);
        listenersContainer.setSessionLiveCheckInterval(100L);
        listenersContainer.setSessionLiveCheckIntervalOnStartup(200L);
        listenersContainer.setEventListenerItems(eventListenerItems);
        
        assertEquals(0, observationManager.size());
        listenersContainer.start();
        Thread.sleep(200L);
        assertEquals(eventListenerItems.size(), observationManager.size());
        listenersContainer.stop();
        assertEquals(0, observationManager.size());
    }
    
    @Test
    public void testSessionCheckingLifecycleMultiThreaded() throws Exception {
        List<EventListenerItem> eventListenerItems = new ArrayList<EventListenerItem>();
        
        eventListenerItems.add(createEventListenerItem("/hst:configurations"));
        
        EventListenersContainerImpl listenersContainer = new EventListenersContainerImpl();
        listenersContainer.setRepository(repository);
        listenersContainer.setCredentials(credentials);
        listenersContainer.setSessionLiveCheck(true);
        listenersContainer.setSessionLiveCheckInterval(100L);
        listenersContainer.setSessionLiveCheckIntervalOnStartup(200L);
        listenersContainer.setEventListenerItems(eventListenerItems);
        
        assertEquals(0, observationManager.size());
        
        Thread [] threads = new Thread[20];
        
        for (int i = 0; i < 20; i++) {
            threads[i] = new EventListenerItemAddingThread(listenersContainer, "/custompath" + i);
        }
        
        listenersContainer.start();
        
        for (int i = 0; i < 20; i++) {
            threads[i].start();
            Thread.sleep(10);
        }
        
        for (int i = 0; i < 20; i++) {
            threads[i].join();
        }
        
        listenersContainer.stop();
        listenersContainer.start();
        
        Thread.sleep(300L);
        
        assertEquals(eventListenerItems.size(), observationManager.size());
        
        listenersContainer.stop();
        assertEquals(0, observationManager.size());
    }
    
    @Test
    public void testEventListenersContainerSessionChecker() throws Exception {
        List<EventListenerItem> eventListenerItems = new ArrayList<EventListenerItem>();
        
        EventListenersContainerImpl listenersContainer = new EventListenersContainerImpl();
        listenersContainer.setRepository(repository);
        listenersContainer.setCredentials(credentials);
        listenersContainer.setSessionLiveCheck(true);
        listenersContainer.setSessionLiveCheckInterval(100L);
        listenersContainer.setSessionLiveCheckIntervalOnStartup(200L);
        listenersContainer.setEventListenerItems(eventListenerItems);
        
        listenersContainer.start();
        
        ThreadGroup tGroup = listenersContainer.eventListenersContainerSessionChecker.getThreadGroup();
        int activeCount = tGroup.activeCount();
        
        assertNotNull(listenersContainer.eventListenersContainerSessionChecker);
        assertTrue(listenersContainer.eventListenersContainerSessionChecker.isAlive());
        
        listenersContainer.stop();

        assertNull(listenersContainer.eventListenersContainerSessionChecker);
        assertEquals(activeCount - 1, tGroup.activeCount());
    }
    
    private EventListenerItem createEventListenerItem(String absPath) {
        EventListenerItemImpl item = new EventListenerItemImpl();
        item.setAbsolutePath(absPath);
        item.setDeep(true);
        item.setEventTypes(31);
        item.setEventListener(new MockEventListener(absPath));
        return item;
    }
    
    @Ignore
    private class EventListenerItemAddingThread extends Thread {
        
        private EventListenersContainerImpl listenersContainer;
        private String absPath;
        
        private EventListenerItemAddingThread(EventListenersContainerImpl listenersContainer, String absPath) {
            super("EventListenerItemAddingThread for " + absPath);
            this.listenersContainer = listenersContainer;
            this.absPath = absPath;
            setDaemon(false);
        }
        
        public void run() {
            listenersContainer.addEventListenerItem(createEventListenerItem(absPath));
        }
    }
    
    @Ignore
    private class MockObservationManager implements ObservationManager {
        
        private Map<String, EventListener> absPathListenersMap = Collections.synchronizedMap(new HashMap<String, EventListener>());
        
        public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypeName, boolean noLocal) throws RepositoryException {
            absPathListenersMap.put(absPath, listener);
        }

        public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        public void removeEventListener(EventListener listener) throws RepositoryException {
            absPathListenersMap.remove(((MockEventListener) listener).getAbsPath());
        }
        
        public int size() {
            return absPathListenersMap.size();
        }

        public EventJournal getEventJournal() throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep, String[] uuid,
                String[] nodeTypeName) throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setUserData(String userData) throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    @Ignore
    private class MockEventListener implements EventListener {
        
        private String absPath;
        
        private MockEventListener(String absPath) {
            this.absPath = absPath;
        }
        
        public void onEvent(EventIterator events) {
        }
        
        public String getAbsPath() {
            return absPath;
        }
    }
    
}
