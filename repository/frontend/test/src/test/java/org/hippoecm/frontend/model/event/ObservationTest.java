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
package org.hippoecm.frontend.model.event;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.wicket.Session;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.api.HippoNode;
import org.junit.Test;

public class ObservationTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final class TestWriter implements Runnable {
        volatile boolean stop = false;
        volatile boolean running = false;

        javax.jcr.Session writerSession;

        TestWriter() throws LoginException, RepositoryException {
            writerSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        }

        void stop() {
            stop = true;
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void run() {
            running = true;
            while (!stop) {
                try {
                    Node testNode = writerSession.getRootNode().getNode("test");
                    testNode.setProperty("a", "b");
                    writerSession.save();
                } catch (InvalidItemStateException ex) {
                    // this is to be expected, as there will be concurrent modifications
                    try {
                        writerSession.refresh(false);
                    } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } catch (RepositoryException ex) {
                    ex.printStackTrace();
                }
            }
            writerSession.logout();
            running = false;
        }
    }

    private class TestObservable implements IObservable {
        private static final long serialVersionUID = 1L;

        private IObservationContext<IObservable> context;
        int identity = 12345;

        @SuppressWarnings("unchecked")
        public void setObservationContext(IObservationContext<?> context) {
            this.context = (IObservationContext<IObservable>) context;
        }

        public void startObservation() {
        }

        public void stopObservation() {
        }

        void fire() {
            EventCollection<IEvent<IObservable>> collection = new EventCollection<IEvent<IObservable>>();
            collection.add(new IEvent<IObservable>() {

                public IObservable getSource() {
                    return TestObservable.this;
                }

            });
            context.notifyObservers(collection);
        }

        @Override
        public int hashCode() {
            return identity;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof TestObservable) && (((TestObservable) obj).identity == identity);
        }
    }

    private class TestObserver implements IObserver<IObservable> {
        private static final long serialVersionUID = 1L;

        private List<IEvent> events;
        private IObservable model;

        TestObserver(IObservable model, List<IEvent> events) {
            this.events = events;
            this.model = model;
        }

        public IObservable getObservable() {
            return model;
        }

        public void onEvent(Iterator<? extends IEvent<IObservable>> iter) {
            while (iter.hasNext()) {
                events.add(iter.next());
            }
        }

    }

    @Test
    public void testObservable() throws Exception {
        TestObservable observable = new TestObservable();
        List<IEvent> events = new LinkedList<IEvent>();
        context.registerService(new TestObserver(observable, events), IObserver.class.getName());

        observable.fire();
        assertEquals(1, events.size());
    }

    @Test
    public void testEquivalence() throws Exception {
        TestObservable observableA = new TestObservable();
        List<IEvent> eventsA = new LinkedList<IEvent>();
        context.registerService(new TestObserver(observableA, eventsA), IObserver.class.getName());

        TestObservable observableB = new TestObservable();
        List<IEvent> eventsB = new LinkedList<IEvent>();
        context.registerService(new TestObserver(observableB, eventsB), IObserver.class.getName());

        observableA.fire();
        assertEquals(1, eventsB.size());
        assertEquals(1, eventsA.size());

        observableB.fire();
        assertEquals(2, eventsB.size());
        assertEquals(2, eventsA.size());
    }

    @Test
    public void testObservableIdentityChange() throws Exception {
        TestObservable observable = new TestObservable();
        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(observable, events);
        context.registerService(observer, IObserver.class.getName());
        observable.identity = 23456;
        context.unregisterService(observer, IObserver.class.getName());
    }

    @Test
    public void testJcrObservable() throws Exception {
        Node root = session.getRootNode();
        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(root), events);
        context.registerService(observer, IObserver.class.getName());

        // when a node is added, observer should be notified

        root.addNode("test", "nt:unstructured");

        // in-session event
        home.processEvents();
        assertEquals(1, events.size());

        // shouldn't receive new event on next processing
        home.processEvents();
        assertEquals(1, events.size());

        session.save();

        Thread.sleep(1000);
        home.processEvents();

        // "out-of-session" event
        assertEquals(2, events.size());

        context.unregisterService(observer, IObserver.class.getName());

        // after unregistering, no events should be received

        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        home.processEvents();

        assertEquals(2, events.size());
    }

    @Test
    public void testNewNodeObservation() throws Exception {
        Node root = session.getRootNode();
        Node test = root.addNode("test", "nt:unstructured");

        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(test), events);
        context.registerService(observer, IObserver.class.getName());

        home.processEvents();

        assertEquals(0, events.size());

        test.addNode("sub", "nt:unstructured");

        home.processEvents();

        assertEquals(1, events.size());
        Event event = ((JcrEvent) events.get(0)).getEvent();
        assertEquals(Event.NODE_ADDED, event.getType());
        assertEquals("/test/sub", event.getPath());
    }

    @Test
    public void testReferenceableNodeObservation() throws Exception {
        Node test = root.addNode("test", "nt:unstructured");
        test.addMixin("mix:referenceable");
        session.save();

        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(test), events);
        context.registerService(observer, IObserver.class.getName());

        test.setProperty("a", "b");
        home.processEvents();
        assertEquals(1, events.size());
    }

    @Test
    public void testInSessionEventSuppression() throws Exception {
        Node root = session.getRootNode();
        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(root), events);
        context.registerService(observer, IObserver.class.getName());

        // when a node is added, observer should be notified

        Node testNode = root.addNode("test", "nt:unstructured");

        // shouldn't receive new event on next processing
        home.processEvents();
        assertEquals(1, events.size());

        testNode.setProperty("test", "bla");

        home.processEvents();
        assertEquals(1, events.size());

        home.processEvents();
        assertEquals(1, events.size());

        testNode.setProperty("test", "die");

        home.processEvents();
        assertEquals(1, events.size());

        home.processEvents();
        assertEquals(1, events.size());

        session.save();
    }

    class TreeObservable implements IObservable {
        private static final long serialVersionUID = 1L;

        private IObservationContext observationContext;
        private JcrEventListener listener;

        public void setObservationContext(IObservationContext context) {
            this.observationContext = context;
        }

        public void startObservation() {
            listener = new JcrEventListener(observationContext, Event.NODE_ADDED | Event.NODE_REMOVED, "/", true, null,
                    null);
            listener.start();
        }

        public void stopObservation() {
            if (listener != null) {
                listener.stop();
                listener = null;
            }
        }

    }

    @Test
    public void testInSessionEventTypes() throws Exception {
        Node root = session.getRootNode();
        Node testNode = root.addNode("test", "nt:unstructured");
        session.save();

        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new TreeObservable(), events);
        context.registerService(observer, IObserver.class.getName());

        // verify that a new node leads to a "node changed" event on the parent
        Node newNode = testNode.addNode("new", "nt:unstructured");
        home.processEvents();
        assertEquals(2, events.size());
        {
            JcrEvent jcrEvent = (JcrEvent) events.get(0);
            Event event = jcrEvent.getEvent();
            assertEquals(0, event.getType());
            assertEquals("/test", event.getPath());
        }
        events.clear();
    }

    @Test
    public void testInterSessionCommunication() throws Exception {
        Node root = session.getRootNode();
        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(root), events);
        context.registerService(observer, IObserver.class.getName());

        // create node in other session and verify that it is picked up as an event
        javax.jcr.Session other = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node otherTestNode = other.getRootNode().addNode("test", "nt:unstructured");
        other.save();

        Thread.sleep(500);

        home.processEvents();
        assertEquals(1, events.size());

        Node testNode = root.getNode("test");
        assertTrue(testNode.isSame(otherTestNode));

        // remove the node in the other session; verify that change is seen
        otherTestNode.remove();
        other.save();
    }

    @Test
    /**
     * test whether event listeners are garbage collected.
     */
    public void testListenerEviction() throws Exception {
        Node root = session.getRootNode();
        List<IEvent> events = new LinkedList<IEvent>();
        context.registerService(new TestObserver(new JcrNodeModel(root), events), IObserver.class.getName());

        // remove all references
        Session.get().getDefaultPageMap().remove(home);
        // need to do this twice, test application maintains a reference to the previously rendered page
        home = tester.startPluginPage();
        home = tester.startPluginPage();
        context = new PluginContext(home.getPluginManager(), new JavaPluginConfig("test"));
        System.gc();

        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        home.processEvents();

        assertEquals(0, events.size());
    }

    private static class SerializationTestContext implements Serializable {
        private static final long serialVersionUID = 1L;

        int count = 0;
        JcrEventListener listener = new JcrEventListener(new IObservationContext<JcrNodeModel>() {
            private static final long serialVersionUID = 1L;

            public void notifyObservers(EventCollection<IEvent<JcrNodeModel>> event) {
                count++;
            }

            public void registerObserver(IObserver observer) {
                // TODO Auto-generated method stub

            }

            public void unregisterObserver(IObserver observer) {
                // TODO Auto-generated method stub

            }

        }, Event.NODE_ADDED, "/", false, null, null);
    }

    @Test
    /**
     * test whether deserialized event listeners re-register
     */
    public void testListenerSerialization() throws Exception {
        Node root = session.getRootNode();
        SerializationTestContext original = new SerializationTestContext();
        original.listener.start();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(original);

        InputStream is = new ByteArrayInputStream(os.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(is);
        SerializationTestContext copy = (SerializationTestContext) ois.readObject();

        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        home.processEvents();

        assertEquals(1, copy.count);
    }

    @Test
    /**
     * test whether events are received on facet search nodes
     */
    public void testFacetSearchEvent() throws Exception {
        Node root = session.getRootNode();
        Node test = root.addNode("test", "nt:unstructured");

        Node source = test.addNode("source", "nt:unstructured");
        source.addMixin("mix:referenceable");
        session.save();

        Node sink = test.addNode("sink", "frontendtest:document");
        Node search = sink.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:facets", new String[] { "facet" });
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", source.getUUID());
        session.save();

        final List<IEvent> events = new LinkedList<IEvent>();
        JcrEventListener listener = new JcrEventListener(new IObservationContext<JcrNodeModel>() {
            private static final long serialVersionUID = 1L;

            public void notifyObservers(EventCollection<IEvent<JcrNodeModel>> collection) {
                for (IEvent event : collection) {
                    events.add(event);
                }
            }

            public void registerObserver(IObserver observer) {
                // TODO Auto-generated method stub

            }

            public void unregisterObserver(IObserver observer) {
                // TODO Auto-generated method stub

            }

        }, Event.NODE_ADDED | Event.NODE_REMOVED, "/test/sink", true, null, null);
        listener.start();

        Node xyz = source.addNode("xyz", "frontendtest:document");
        xyz.addMixin("hippo:harddocument");
        xyz.setProperty("facet", "xyz");
        session.save();

        // basic facetsearch assertion
        Node result = sink.getNode("search/xyz/hippo:resultset/xyz");
        assertTrue(((HippoNode) result).getCanonicalNode().isSame(xyz));

        // event should have been received
        home.processEvents();
        assertEquals(1, events.size());

        session.save();
    }

    @Test
    public void testRemoveAdd() throws Exception {
        Node root = session.getRootNode();

        Node testNode = root.addNode("test", "nt:unstructured");
        session.save();

        Node subNode = testNode.addNode("abc");
        session.save();

        List<IEvent> events = new LinkedList<IEvent>();
        JcrNodeModel model = new JcrNodeModel(testNode);
        IObserver observer = new TestObserver(model, events);
        context.registerService(observer, IObserver.class.getName());

        subNode.setProperty("a", "b");
        subNode.remove();
        subNode = testNode.addNode("abc");

        home.processEvents();
        assertEquals(0, events.size());
    }

    @Test
    public void testFixedNodeMonitor() throws Exception {
        Node testNode = session.getRootNode().addNode("test");
        session.save();

        List<IEvent> events = new LinkedList<IEvent>();
        JcrNodeModel model = new JcrNodeModel(testNode);
        IObserver observer = new TestObserver(model, events);
        context.registerService(observer, IObserver.class.getName());

        home.processEvents();
        testNode.setProperty("a", "b");

        home.processEvents();
        assertEquals(1, events.size());
        JcrEvent jcrEvent = (JcrEvent) events.get(0);
        assertEquals(Event.PROPERTY_ADDED, jcrEvent.getEvent().getType());
    }

    /*    @Test
        public void testReordering() throws Exception {
            Node testNode = session.getRootNode().addNode("test", "frontendtest:ordered");
            Node childOne = testNode.addNode("frontendtest:childnode", "nt:unstructured");
            Node childTwo = testNode.addNode("frontendtest:childnode", "nt:unstructured");
            session.save();
            
            List<IEvent> events = new LinkedList<IEvent>();
            JcrNodeModel model = new JcrNodeModel(testNode);
            IObserver observer = new TestObserver(model, events);
            context.registerService(observer, IObserver.class.getName());

            home.processEvents();
            assertEquals(0, events.size());

            testNode.orderBefore("frontendtest:childnode[2]", "frontendtest:childnode[1]");
            session.save();
            
            home.processEvents();
            assertEquals(2, events.size());
        }
    */

    @Test
    public void testWritingListenerDoesntDeadlock() throws Exception {
        Node testNode = session.getRootNode().addNode("test");
        session.save();

        final JcrNodeModel model = new JcrNodeModel(testNode);
        IObserver observer = new IObserver<JcrNodeModel>() {
            private static final long serialVersionUID = 1L;

            public JcrNodeModel getObservable() {
                return model;
            }

            public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> iter) {
                Node node = model.getNode();
                try {
                    Thread.sleep(10);
                    node.setProperty("a", "c");
                    session.save();
                } catch (InvalidItemStateException e) {
                    try {
                        session.refresh(false);
                    } catch (RepositoryException ex) {
                        throw new RuntimeException("Unexpected repository exception", e);
                    } 
                } catch (RepositoryException e) {
                    throw new RuntimeException("Unexpected repository exception", e);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread was interrupted", e);
                }
            }
        };

        context.registerService(observer, IObserver.class.getName());
        
        TestWriter writer = new TestWriter();
        Thread thread = new Thread(writer);
        thread.start();

        // ad-hoc maximum number of retries.
        // Experiments revealed deadlock in the 1 - 300 number of retry range
        for(int i = 0; i < 1000; i++) {
            home.processEvents();
        }

        writer.stop();
    }

}
