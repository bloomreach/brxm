/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.compare;

import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.hippoecm.frontend.types.TypeException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NodeComparerTest extends PluginTest {

    String[] content = {
        "/test", "nt:unstructured",
            "/test/a", "nt:unstructured",
                "x", "aap",
                "y", "noot",
            "/test/b", "nt:unstructured",
                "x", "mies",
                "y", "noot",
            "/test/c", "nt:unstructured",
                "x", "aap",
                "y", "noot",
    };
    
    @Test
    public void testNodes() throws RepositoryException, TypeException {
        build(session, content);

        ITypeLocator typeLocator = new ITypeLocator() {

            public List<ITypeDescriptor> getSubTypes(String type) {
                return Collections.emptyList();
            }

            public ITypeDescriptor locate(String type) {
                return null;
            }

            public void detach() {
            }

        };
        ITypeDescriptor stringDescriptor = new JavaTypeDescriptor("String", "string", null);
        stringDescriptor.setIsNode(false);

        ITypeDescriptor descriptor = new JavaTypeDescriptor("test", "hippo:test", typeLocator);
        JavaFieldDescriptor field = new JavaFieldDescriptor("hippo", stringDescriptor);
        field.setName("x");
        field.setPath("x");
        descriptor.addField(field);
        field = new JavaFieldDescriptor("hippo", stringDescriptor);
        field.setName("y");
        field.setPath("y");
        descriptor.addField(field);

        NodeComparer comparer = new NodeComparer(descriptor, null);
        assertFalse(comparer.areEqual(root.getNode("test/a"), root.getNode("test/b")));
        assertNotEquals(comparer.getHashCode(root.getNode("test/a")), comparer.getHashCode(root.getNode("test/b")));

        assertTrue(comparer.areEqual(root.getNode("test/a"), root.getNode("test/c")));
        assertEquals(comparer.getHashCode(root.getNode("test/a")), comparer.getHashCode(root.getNode("test/c")));
    }

}
