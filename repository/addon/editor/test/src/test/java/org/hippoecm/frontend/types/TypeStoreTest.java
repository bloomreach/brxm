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
package org.hippoecm.frontend.types;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.Test;

public class TypeStoreTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    HippoTester tester;
    Home home;
    IPluginContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp(true);
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

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testJcrTypeDescriptor() {
        // need session context
        HippoTester tester = new HippoTester(new JcrSessionModel(new ValueMap("username=admin,password=admin")) {
            @Override
            protected Object load() {
                return session;
            }
        });

        IStore<ITypeDescriptor> typeStore = new JcrTypeStore(context);
        ITypeDescriptor type = typeStore.load("test:test");
        assertEquals("test:test", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertTrue(fields.size() == 2);
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("child"));

        IFieldDescriptor title = fields.get("title");
        assertEquals("String", title.getType());
        assertEquals("test:title", title.getPath());
        assertEquals("title", title.getName());
    }

    @Test
    public void testBuiltinTypeDescriptor() {
        // need session context
        HippoTester tester = new HippoTester(new JcrSessionModel(new ValueMap("username=admin,password=admin")) {
            @Override
            protected Object load() {
                return session;
            }
        });

        IStore<ITypeDescriptor> typeStore = new BuiltinTypeStore();
        ITypeDescriptor type = typeStore.load("test:test2");
        assertEquals("test:test2", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertTrue(fields.size() == 2);
        assertTrue(fields.keySet().contains("test:title"));
        assertTrue(fields.keySet().contains("test:child"));

        IFieldDescriptor title = fields.get("test:title");
        assertEquals("String", title.getType());
        assertEquals("test:title", title.getPath());
    }

    @Test
    public void testJcrTypeSave() throws Exception {
        // need session context
        HippoTester tester = new HippoTester(new JcrSessionModel(new ValueMap("username=admin,password=admin")) {
            @Override
            protected Object load() {
                return session;
            }
        });

        IStore<ITypeDescriptor> jcrTypeStore = new JcrTypeStore(context);

        IStore<ITypeDescriptor> typeStore = new BuiltinTypeStore();
        ITypeDescriptor builtinType = typeStore.load("test:test2");
        String titleName = null;
        for (IFieldDescriptor field : builtinType.getFields().values()) {
            if (field.getPath().equals("test:title")) {
                titleName = field.getName();
            }
        }
        assertTrue(titleName != null);

        jcrTypeStore.save(builtinType);

        ITypeDescriptor type = jcrTypeStore.load("test:test2");
        assertEquals("test:test2", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertTrue(fields.size() == 2);
        Set<String> expected = new HashSet<String>();
        expected.add("test:title");
        expected.add("test:child");
        IFieldDescriptor title = null;
        for (IFieldDescriptor field : fields.values()) {
            expected.remove(field.getPath());
            if (field.getPath().equals("test:title")) {
                title = field;
            }
        }
        assertEquals(0, expected.size());
        assertTrue(title != null);

        assertEquals("String", title.getType());
        assertEquals("test:title", title.getPath());
        assertEquals(titleName, title.getName());
    }

}
