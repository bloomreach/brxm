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
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.ocm.IStore;
import org.junit.Test;

public class TypeStoreTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testJcrTypeDescriptor() throws Exception {
        IStore<ITypeDescriptor> typeStore = new JcrTypeStore();
        ITypeDescriptor type = typeStore.load("test:test");
        assertEquals("test:test", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("child"));

        IFieldDescriptor title = fields.get("title");
        assertEquals("String", title.getType());
        assertEquals("test:title", title.getPath());
        assertEquals("title", title.getName());
    }

    @Test
    public void testBuiltinTypeDescriptor() throws Exception {
        IStore<ITypeDescriptor> typeStore = new BuiltinTypeStore();
        ITypeDescriptor type = typeStore.load("test:test2");
        assertEquals("test:test2", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.keySet().contains("test:title"));
        assertTrue(fields.keySet().contains("test:child"));

        IFieldDescriptor title = fields.get("test:title");
        assertEquals("String", title.getType());
        assertEquals("test:title", title.getPath());
    }

    @Test
    public void testJcrTypeSave() throws Exception {
        JcrTypeStore jcrTypeStore = new JcrTypeStore();
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
        assertEquals(2, fields.size());
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

    @Test
    public void testHistoricType() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();
        ITypeDescriptor descriptor = typeStore.load("test_0_0:test");
        assertNotNull(descriptor);
    }
    
}
