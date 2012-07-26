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
package org.hippoecm.frontend.model.ocm;

import static junit.framework.Assert.assertEquals;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObjectTest extends PluginTest {

    static final Logger log = LoggerFactory.getLogger(JcrObjectTest.class);

    final static class TestEvent implements IEvent {

        TestObject source;

        TestEvent(TestObject source) {
            this.source = source;
        }

        public IObservable getSource() {
            return source;
        }

    }

    class TestObject extends JcrObject {
        private static final long serialVersionUID = 1L;

        public TestObject(JcrNodeModel nodeModel) {
            super(nodeModel);
        }

        void setValue(String value) {
            try {
                Node node = getNode();
                node.setProperty("value", value);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        @Override
        protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
            context.notifyObservers(new EventCollection());
        }
    }

    class TestObserver extends Observer<IObservable> {
        private static final long serialVersionUID = 1L;

        int count = 0;

        TestObserver(IObservable observable) {
            super(observable);
        }

        public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
            count++;
        }
    };

    @Test
    public void testNotification() throws Exception {
        Node root = session.getRootNode();
        Node test = root.addNode("test");

        TestObject testObject = new TestObject(new JcrNodeModel(test));
        TestObserver observer = new TestObserver(testObject);
        context.registerService(observer, IObserver.class.getName());
        testObject.setValue("testing 1 2 3");

        home.processEvents();

        assertEquals(1, observer.count);

        testObject.setValue("bladie");

        home.processEvents();

        assertEquals(2, observer.count);
    }

}
