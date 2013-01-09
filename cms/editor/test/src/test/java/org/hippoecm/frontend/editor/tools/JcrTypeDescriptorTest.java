/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.tools;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.hippoecm.editor.type.JcrTypeDescriptor;
import org.hippoecm.editor.type.JcrTypeStore;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.junit.Test;

public class JcrTypeDescriptorTest extends PluginTest {

    @Test
    public void testAllFieldsReturned() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();

        ITypeDescriptor descriptor = typeStore.getTypeDescriptor("test:inheriting");
        assertEquals(1, descriptor.getDeclaredFields().size());

        IFieldDescriptor field = descriptor.getDeclaredFields().values().iterator().next();
        assertEquals("extra", field.getName());
        assertEquals("String", field.getTypeDescriptor().getType());

        Map<String, IFieldDescriptor> fields = descriptor.getFields();
        assertEquals(3, fields.size());
    }

    @Test
    public void testSerialization() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();

        ITypeDescriptor descriptor = typeStore.getTypeDescriptor("test:test");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(descriptor);

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(is);
        JcrTypeDescriptor clone = (JcrTypeDescriptor) ois.readObject();
        assertEquals(2, clone.getFields().size());
    }
    
}
