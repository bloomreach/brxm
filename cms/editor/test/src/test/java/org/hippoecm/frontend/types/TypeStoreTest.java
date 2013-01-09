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
package org.hippoecm.frontend.types;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.EditorTestCase;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.ocm.IStore;
import org.junit.Test;

public class TypeStoreTest extends EditorTestCase {

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
        assertEquals("String", title.getTypeDescriptor().getName());
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
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("child"));

        IFieldDescriptor title = fields.get("title");
        assertEquals("String", title.getTypeDescriptor().getName());
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

        ITypeDescriptor type = jcrTypeStore.getDraftType("test:test2");
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

        assertEquals("String", title.getTypeDescriptor().getName());
        assertEquals("test:title", title.getPath());
        assertEquals(titleName, title.getName());
    }

    @Test
    public void testHistoricType() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();
        ITypeDescriptor descriptor = typeStore.load("test_0_0:test");
        assertNotNull(descriptor);
        assertEquals("test:test", descriptor.getType());

        IFieldDescriptor field = descriptor.getField("title");
        assertEquals("test:title", field.getPath());

        ITypeDescriptor inheriting = typeStore.load("test_0_0:inheriting");
        List<String> superTypes = inheriting.getSuperTypes();
        assertEquals(1, superTypes.size());
        assertEquals("test:test", superTypes.get(0));
    }

    @Test
    public void testLegacyFormat() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();
        ITypeDescriptor descriptor = typeStore.load("test:legacy");
        assertNotNull(descriptor);
        assertEquals("test:legacy", descriptor.getType());

        assertEquals(2, descriptor.getFields().size());
        
        IFieldDescriptor field = descriptor.getField("a");
        assertNotNull(field);
        assertEquals("a", field.getName());

        field = descriptor.getField("b");
        assertNotNull(field);
        assertEquals("b", field.getName());
    }

    @Test
    public void testPseudoType() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();
        ITypeDescriptor descriptor = typeStore.load("test:pseudo");
        assertNotNull(descriptor);
        assertEquals("test:test", descriptor.getType());

        Map<String, IFieldDescriptor> fields = descriptor.getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("child"));
    }

    @Test
    public void testFetchDraft() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();
        ITypeDescriptor type = typeStore.getDraftType("test:edited");
        assertEquals("test:edited", type.getName());

        Map<String, IFieldDescriptor> fields = type.getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("child"));

        IFieldDescriptor title = fields.get("title");
        assertEquals("String", title.getTypeDescriptor().getName());
        assertEquals("test:title", title.getPath());

        // current type should be the builtin descriptor
        type = typeStore.getTypeLocator().locate("test:edited");
        fields = type.getFields();
        assertEquals(1, fields.size());
        assertTrue(fields.keySet().contains("title"));
    }

}
