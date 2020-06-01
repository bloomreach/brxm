/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmsEventDispatcherServiceTest extends PluginTest {

    private Node testNode;
    private List<IEvent> propertyModelEvents = new ArrayList<IEvent>();
    private List<IEvent> nodeModelEvents = new ArrayList<IEvent>();
    private JcrNodeModel nodeModel;
    private JcrPropertyModel propModel;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        testNode = root.addNode("test");
        Property prop = testNode.setProperty("property", "value");
        home.processEvents();
        propModel = new JcrPropertyModel(prop);
        context.registerService(new IObserver<JcrPropertyModel>() {
            private static final long serialVersionUID = 1L;

            public JcrPropertyModel getObservable() {
                return propModel;
            }

            public void onEvent(Iterator<? extends IEvent<JcrPropertyModel>> events) {
                while (events.hasNext()) {
                    propertyModelEvents.add(events.next());
                }
            }
        }, IObserver.class.getName());

        nodeModel = new JcrNodeModel(testNode);
        context.registerService(new IObserver<JcrNodeModel>() {
            private static final long serialVersionUID = 1L;

            public JcrNodeModel getObservable() {
                return nodeModel;
            }

            public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> events) {
                while (events.hasNext()) {
                    nodeModelEvents.add(events.next());
                }
            }
        }, IObserver.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        propertyModelEvents.clear();
        nodeModelEvents.clear();
        super.tearDown();
    }

    @Test
    public void manual_dispatching_event_makes_models_receive_event() throws Exception {
        cmsEventDispatcherService.events(testNode);
        home.processEvents();
        assertEquals(1, propertyModelEvents.size());
        final JcrEvent propertyModelEvent = (JcrEvent) propertyModelEvents.get(0);
        assertEquals("/test", propertyModelEvent.getEvent().getPath());

        assertEquals(1, nodeModelEvents.size());
        final JcrEvent nodeModelEvent = (JcrEvent) nodeModelEvents.get(0);
        assertEquals("/test", nodeModelEvent.getEvent().getPath());
    }

    @Test
    public void duplicate_events_for_same_node_count_once_for_models() throws Exception {
        cmsEventDispatcherService.events(testNode, testNode, testNode);
        home.processEvents();

        assertEquals(1, propertyModelEvents.size());
        final JcrEvent propertyModelEvent = (JcrEvent) propertyModelEvents.get(0);
        assertEquals("/test", propertyModelEvent.getEvent().getPath());

        assertEquals(1, nodeModelEvents.size());
        final JcrEvent nodeModelEvent = (JcrEvent) nodeModelEvents.get(0);
        assertEquals("/test", nodeModelEvent.getEvent().getPath());
    }

    @Test
    public void multiple_events_for_different_nodes_results_in_only_event_for_right_models() throws Exception {
        cmsEventDispatcherService.events(testNode, root);
        home.processEvents();
        assertEquals("Even though there are two nodes dispatched, the property model should only receive " +
                "events for the node the property belongs to", 1, propertyModelEvents.size());
        final JcrEvent event = (JcrEvent) propertyModelEvents.get(0);
        assertEquals("/test", event.getEvent().getPath());

        assertEquals("Even though there are two nodes dispatched, the node model should only receive " +
                "events for the node the property belongs to", 1, nodeModelEvents.size());
        final JcrEvent nodeModelEvent = (JcrEvent) nodeModelEvents.get(0);
        assertEquals("/test", nodeModelEvent.getEvent().getPath());

    }

}