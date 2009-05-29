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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.junit.Test;

public class JcrPropertyValueModelTest extends PluginTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Test
    public void testConstructors() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        Value value = createValue("x");
        Property prop = test.setProperty("frontendtest:strings", new Value[] { value });

        // let prop model create the value model
        JcrPropertyModel propModel = new JcrPropertyModel(prop);
        assertEquals(1, propModel.size());
        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) propModel.model(propModel.iterator(0, 1).next());
        assertEquals("x", valueModel.getObject());

        // create model by hand
        valueModel = new JcrPropertyValueModel(0, value, propModel);
        assertEquals("x", valueModel.getObject());

        // single-valued prop
        prop = test.setProperty("frontendtest:string", createValue("y"));
        propModel = new JcrPropertyModel(prop);

        // create model by hand
        valueModel = new JcrPropertyValueModel(propModel);
        assertEquals("y", valueModel.getObject());
    }

    @Test
    public void testMultiple() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        Property prop = test.setProperty("frontendtest:strings", new Value[] { createValue("x") });

        JcrPropertyModel propModel = new JcrPropertyModel(prop);
        JcrPropertyValueModel valueModel = (JcrPropertyValueModel) propModel.model(propModel.iterator(0, 1).next());
        assertEquals("x", valueModel.getObject());

        propModel.detach();

        test.setProperty("frontendtest:strings", new Value[] { createValue("x"), createValue("y") });
        assertEquals(2, propModel.size());
        JcrPropertyValueModel[] models = new JcrPropertyValueModel[2];
        Iterator iter = propModel.iterator(0, 2);
        for (int i = 0; i < 2; i++) {
            models[i] = (JcrPropertyValueModel) propModel.model(iter.next());
        }
        assertEquals("x", models[0].getObject());
        assertEquals("y", models[1].getObject());

        // detach cycle
        for (int i = 0; i < 2; i++) {
            models[i].detach();
        }
        assertEquals("x", models[0].getObject());
        assertEquals("y", models[1].getObject());
    }

    @Test
    public void testNonExisting() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/frontendtest:string");
        JcrPropertyValueModel valueModel = new JcrPropertyValueModel(propModel);
        valueModel.setObject("y");
    }

    @Test
    public void testIllegalProperty() throws Exception {
        Node test = this.root.addNode("test", "frontendtest:model");
        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/frontendtest:invalid");
        JcrPropertyValueModel valueModel = new JcrPropertyValueModel(propModel);
        valueModel.setObject(Boolean.TRUE);
        valueModel.detach();
        assertEquals(null, valueModel.getObject());
    }

    protected Value createValue(String value) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory().createValue(value);
    }

}
