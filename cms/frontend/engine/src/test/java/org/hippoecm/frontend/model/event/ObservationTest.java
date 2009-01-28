package org.hippoecm.frontend.model.event;

import static junit.framework.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.Session;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.JcrObservationManager;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.TestCase;
import org.junit.Test;

public class ObservationTest extends TestCase {

    private class TestObservable implements IObservable {
        private static final long serialVersionUID = 1L;

        private IObservationContext context;

        public void setObservationContext(IObservationContext context) {
            this.context = context;
        }

        public void startObservation() {
        }

        public void stopObservation() {
        }

        void fire() {
            context.publish(new IEvent() {

                public IObservable getSource() {
                    return TestObservable.this;
                }

            });
        }

        @Override
        public int hashCode() {
            return 12345;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestObservable;
        }
    }

    private class TestObserver implements IObserver {
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

        public void onEvent(IEvent event) {
            events.add(event);
        }

    }

    HippoTester tester;
    Home home;
    IPluginContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        JcrSessionModel sessionModel = new JcrSessionModel(Main.DEFAULT_CREDENTIALS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }
        };
        tester = new HippoTester(sessionModel);
        home = (Home) tester.startPage(Home.class);
        context = new PluginContext(home.getPluginManager(), "test", null);
    }

    @Test
    public void testObservable() throws Exception {
        TestObservable observable = new TestObservable();
        List<IEvent> events = new LinkedList<IEvent>();
        context.registerService(new TestObserver(observable, events), IObserver.class.getName());

        observable.fire();
        assertTrue(events.size() == 1);
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
        assertTrue(eventsB.size() == 1);
        assertTrue(eventsA.size() == 1);

        observableB.fire();
        assertTrue(eventsB.size() == 2);
        assertTrue(eventsA.size() == 2);
    }

    @Test
    public void testJcrObservable() throws Exception {
        Node root = session.getRootNode();
        List<IEvent> events = new LinkedList<IEvent>();
        IObserver observer = new TestObserver(new JcrNodeModel(root), events);
        context.registerService(observer, IObserver.class.getName());

        // when a node is added, observer should be notified
        
        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        JcrObservationManager.getInstance().process();

        assertTrue(events.size() == 1);

        context.unregisterService(observer, IObserver.class.getName());

        // after unregistering, no events should be received

        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        JcrObservationManager.getInstance().process();

        assertTrue(events.size() == 1);
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
        home = (Home) tester.startPage(Home.class);
        home = (Home) tester.startPage(Home.class);
        context = new PluginContext(home.getPluginManager(), "test", null);
        System.gc();

        root.addNode("test", "nt:unstructured");
        session.save();

        Thread.sleep(1000);
        JcrObservationManager.getInstance().process();

        assertTrue(events.size() == 0);
    }

}
