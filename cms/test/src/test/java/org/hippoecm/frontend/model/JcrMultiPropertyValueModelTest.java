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
package org.hippoecm.frontend.model;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.properties.JcrMultiPropertyValueModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JcrMultiPropertyValueModelTest extends PluginTest {

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

    @Test
    public void testNonExistingPropertyIsCreated() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");

        JcrPropertyModel propModel = new JcrPropertyModel("/test/frontendtest:strings");
        JcrMultiPropertyValueModel valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> list = new ArrayList<String>(1);
        list.add("y");
        valueModel.setObject(list);

        Property prop = test.getProperty("frontendtest:strings");
        Value[] values = prop.getValues();
        assertEquals(1, values.length);
        assertEquals("y", values[0].getString());
    }

    @Test
    public void testSingleValuedValueInRelaxedType() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:relaxed");
        test.setProperty("test", "aap");

        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/test");
        JcrMultiPropertyValueModel<String> valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> values = valueModel.getObject();
        assertEquals(1, values.size());
        assertEquals("aap", values.get(0));
        values.add("noot");
        valueModel.setObject(values);

        Property property = test.getProperty("test");
        assertTrue(property.isMultiple());
        assertEquals(2, property.getValues().length);
    }

    @Test
    public void testNonExistingRelaxedPropertyIsCreated() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:relaxed");

        JcrPropertyModel propModel = new JcrPropertyModel("/test/strings");
        JcrMultiPropertyValueModel valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> list = new ArrayList<String>(1);
        list.add("y");
        valueModel.setObject(list);

        Property prop = test.getProperty("strings");
        Value[] values = prop.getValues();
        assertEquals(1, values.length);
        assertEquals("y", values[0].getString());
    }

    @Test
    public void testEmptyNonExistingRelaxedPropertyIsCreated() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:relaxed");

        JcrPropertyModel propModel = new JcrPropertyModel("/test/strings");
        JcrMultiPropertyValueModel valueModel = new JcrMultiPropertyValueModel<String>(propModel.getItemModel());
        List<String> list = new ArrayList<String>(1);
        valueModel.setObject(list);

        Property prop = test.getProperty("strings");
        Value[] values = prop.getValues();
        assertEquals(0, values.length);
        assertEquals(PropertyType.STRING, prop.getType());
    }

    protected Value createValue(String value) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory().createValue(value);
    }

}
