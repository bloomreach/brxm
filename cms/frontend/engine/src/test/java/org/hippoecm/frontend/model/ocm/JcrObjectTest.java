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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.JcrObservationManager;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObjectTest extends TestCase {

    static final Logger log = LoggerFactory.getLogger(JcrObjectTest.class);

    interface IListener {

        void objectChanged();
    }

    class TestObject extends JcrObject {
        private static final long serialVersionUID = 1L;

        List<IListener> listeners = new LinkedList<IListener>();

        public TestObject(JcrNodeModel nodeModel, IPluginContext context) {
            super(nodeModel, context);
            init();
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
        protected void onEvent(IEvent event) {
            for (IListener listener : listeners) {
                listener.objectChanged();
            }
        }

        void addListener(IListener listener) {
            listeners.add(listener);
        }
    }

    class TestListener implements IListener {
        int count = 0;

        public void objectChanged() {
            count++;
        }
    };

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
        context = new PluginContext(home.getPluginManager(), new JavaPluginConfig("test"));
    }

    @Test
    public void testNotification() throws Exception {
        Node root = session.getRootNode();
        Node test = root.addNode("test");

        TestObject testObject = new TestObject(new JcrNodeModel(test), context);

        TestListener listener = new TestListener();
        testObject.addListener(listener);
        testObject.setValue("testing 1 2 3");
        
        JcrObservationManager.getInstance().processEvents();

        assertEquals(1, listener.count);

        testObject.setValue("bladie");

        JcrObservationManager.getInstance().processEvents();

        assertEquals(2, listener.count);
    }

}
