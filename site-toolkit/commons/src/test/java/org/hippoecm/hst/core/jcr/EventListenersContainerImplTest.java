/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.jcr;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;

/**
 * Tests @{link EventListenersContainerImpl}.
 */
public class EventListenersContainerImplTest {

    @Test
    public void whenAddingEventListenerThrowsExceptionOtherListenersAreStillRegistered() throws RepositoryException {
        Repository mockRepository = EasyMock.createMock(Repository.class);
        Session mockSession = EasyMock.createMock(Session.class);
        Workspace mockWorkSpace = EasyMock.createMock(Workspace.class);
        ObservationManager mockObservationManager = EasyMock.createMock(ObservationManager.class);

        EventListenerItem badListenerItem = EasyMock.createNiceMock(EventListenerItem.class);
        EventListenerItem goodListenerItem = EasyMock.createNiceMock(EventListenerItem.class);
        EventListener badListener = EasyMock.createNiceMock(EventListener.class);
        EventListener goodListener = EasyMock.createNiceMock(EventListener.class);

        List<EventListenerItem> firstBadThenGoodListenerItem = Arrays.asList(badListenerItem, goodListenerItem);

        EventListenersContainerImpl container = new EventListenersContainerImpl("testcontainer");
        container.setRepository(mockRepository);
        container.setEventListenerItems(firstBadThenGoodListenerItem);

        // get observation manager
        EasyMock.expect(mockRepository.login()).andReturn(mockSession);
        EasyMock.expect(mockSession.getWorkspace()).andReturn(mockWorkSpace);
        EasyMock.expect(mockWorkSpace.getObservationManager()).andReturn(mockObservationManager);

        // register a bad listener that throws an exception
        EasyMock.expect(badListenerItem.getEventListener()).andReturn(badListener);
        EasyMock.expect(badListenerItem.getEventTypes()).andReturn(1);
        mockObservationManager.addEventListener(eq(badListener), anyInt(), anyObject(String.class), anyBoolean(), anyObject(String[].class), anyObject(String[].class), anyBoolean());
        EasyMock.expectLastCall().andThrow(new RepositoryException("Generated test error"));

        // register a good listener
        EasyMock.expect(goodListenerItem.getEventListener()).andReturn(goodListener);
        EasyMock.expect(goodListenerItem.getEventTypes()).andReturn(1);
        EasyMock.expect(mockSession.itemExists(anyObject(String.class))).andReturn(false);
        mockObservationManager.addEventListener(eq(goodListener), anyInt(), anyObject(String.class), anyBoolean(), anyObject(String[].class), anyObject(String[].class), anyBoolean());

        // replay
        EasyMock.replay(mockRepository, mockSession, mockWorkSpace, mockObservationManager, badListenerItem, goodListenerItem, badListener, goodListener);
        container.doInit();

        // verify that the good listener is registered even though the bad listener threw an exception
        EasyMock.verify(goodListenerItem);
    }

}
