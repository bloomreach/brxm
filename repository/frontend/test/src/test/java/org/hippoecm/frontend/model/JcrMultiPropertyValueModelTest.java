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
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.properties.JcrMultiPropertyValueModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.junit.Test;

public class JcrMultiPropertyValueModelTest extends PluginTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Test
    public void testIdenticalCollectionInstanceIsReturned() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        Property prop = test.setProperty("frontendtest:strings", new Value[] { createValue("x") });

        JcrPropertyModel propModel = new JcrPropertyModel(prop);
        JcrMultiPropertyValueModel valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> list = valueModel.getObject();
        assertTrue(list == valueModel.getObject());
        assertEquals(1, list.size());
        assertEquals("x", list.get(0));
    }

    @Test
    public void testSetObject() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        Property prop = test.setProperty("frontendtest:strings", new Value[] { createValue("x") });

        JcrPropertyModel propModel = new JcrPropertyModel(prop);
        JcrMultiPropertyValueModel valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> list = valueModel.getObject();
        list.add("y");
        valueModel.setObject(list);

        Value[] values = prop.getValues();
        assertEquals(2, values.length);
        assertEquals("y", values[1].getString());
    }

    protected Value createValue(String value) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory().createValue(value);
    }

}
