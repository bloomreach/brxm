/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.junit.Test;

public class PropertyValueProviderTest extends PluginTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected Value createValue(String value) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory().createValue(value);
    }

    @Test
    public void testAddNewAddsDummyForRequiredField() throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node test = this.root.addNode("test", "frontendtest:model");
        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/frontendtest:value");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:value", new JavaTypeDescriptor("string",
                "String", null));
        field.addValidator("required");

        PropertyValueProvider provider = new PropertyValueProvider(field, field.getTypeDescriptor(), propModel
                .getItemModel());
        provider.addNew();

        assertFalse(session.itemExists(test.getPath() + "/frontendtest:value"));
        assertEquals(1, provider.size());
        JcrPropertyValueModel pvm = provider.iterator(0, 1).next();
        assertEquals(null, pvm.getObject());

        provider.detach();
        assertEquals(1, provider.size());
        pvm = provider.iterator(0, 1).next();
        assertEquals(null, pvm.getObject());
    }
}