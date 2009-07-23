package org.hippoecm.frontend.editor.tools;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.hippoecm.editor.tools.JcrTypeDescriptor;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.junit.Test;

public class JcrTypeDescriptorTest extends PluginTest {

    @Test
    public void testAllFieldsReturned() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();

        JcrTypeDescriptor descriptor = typeStore.getTypeDescriptor("test:inheriting");
        assertEquals(1, descriptor.getDeclaredFields().size());

        IFieldDescriptor field = descriptor.getDeclaredFields().values().iterator().next();
        assertEquals("extra", field.getName());
        assertEquals("String", field.getType());

        Map<String, IFieldDescriptor> fields = descriptor.getFields();
        assertEquals(3, fields.size());
    }

}
