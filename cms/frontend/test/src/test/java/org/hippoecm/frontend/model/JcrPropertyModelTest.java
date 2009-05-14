/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.observation.Event;

import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JcrPropertyModelTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    Node root;
    HippoTester tester;
    Home home;
    IPluginContext context;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        root = session.getRootNode();
        tester = new HippoTester(new JcrSessionModel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }
        });
        home = (Home) tester.startPage(Home.class);
        context = new PluginContext(home.getPluginManager(), new JavaPluginConfig("test"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testEventFiltering() throws Exception {
        Node node = root.addNode("test");
        Property prop = node.setProperty("property", "value");
        home.processEvents();

        final List<IEvent> received = new ArrayList<IEvent>();
        final JcrPropertyModel propModel = new JcrPropertyModel(prop);
        context.registerService(new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return propModel;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                while (events.hasNext()) {
                    received.add(events.next());
                }
            }
        }, IObserver.class.getName());

        JcrEvent event;

        prop.setValue("A");
        propModel.detach();
        home.processEvents();
        assertEquals(1, received.size());
        received.clear();

        prop.remove();
        propModel.detach();
        home.processEvents();
        assertEquals(1, received.size());
        event = (JcrEvent) received.get(0);
        assertEquals(Event.PROPERTY_REMOVED, event.getEvent().getType());
        received.clear();

        prop = node.setProperty("property", "B");
        propModel.detach();
        home.processEvents();
        assertEquals(1, received.size());
        event = (JcrEvent) received.get(0);
        assertEquals(Event.PROPERTY_ADDED, event.getEvent().getType());
        received.clear();

        prop = node.setProperty("property", "C");
        propModel.detach();
        home.processEvents();
        assertEquals(1, received.size());
        event = (JcrEvent) received.get(0);
        assertEquals(Event.PROPERTY_CHANGED, event.getEvent().getType());
        received.clear();
    }

}
