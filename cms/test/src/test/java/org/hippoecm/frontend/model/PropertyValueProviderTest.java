/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link org.hippoecm.frontend.model.PropertyValueProvider}
 */
public class PropertyValueProviderTest extends PluginTest {
    private static final String TEST_NODE_NAME = "test";

    protected Value createValue(String value) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory().createValue(value);
    }

    @Test
    public void testAddNewAddsDummyForRequiredField() throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node test = this.root.addNode(TEST_NODE_NAME, "frontendtest:model");
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

    @Test
    public void testAddNewAddsNoDefaultPropertyForSingleDateField() throws RepositoryException {
        Node testNode = this.root.addNode(TEST_NODE_NAME,"frontendtest:model");
        JcrPropertyModel propertyModel = new JcrPropertyModel(testNode.getPath() + "/frontendtest:date");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:date", new JavaTypeDescriptor("date",
                "Date", null));
        PropertyValueProvider provider = new PropertyValueProvider(field, field.getTypeDescriptor(), propertyModel
                .getItemModel());
        provider.addNew();

        assertEquals("Expected to store null-date value in the date property",
                PropertyValueProvider.NULL_DATE, testNode.getProperty("frontendtest:date").getDate().getTime());

        assertEquals(1, provider.size());
        JcrPropertyValueModel pvm = provider.iterator(0, 1).next();
        assertEquals("Expected to return 'null' for the field storing null-date value", null, pvm.getObject());

        provider.detach();
        assertEquals(1, provider.size());
        pvm = provider.iterator(0, 1).next();
        assertEquals(null, pvm.getObject());
    }

    /**
     * Create a multi-value date property with two empty-values and an initialized value. Expect to return a multi-value
     * date field having an array of two null and an initialized date values.
     *
     * @throws RepositoryException
     */
    @Test
    public void canStoreEmptyValuesInMultiValuesDateField() throws RepositoryException {
        final Date[] dates = new Date[]{null, new Date(), null};
        final int valuesCount = dates.length;

        Node testNode = this.root.addNode(TEST_NODE_NAME,"frontendtest:model");

        JcrPropertyModel propertyModel = new JcrPropertyModel(testNode.getPath() + "/frontendtest:dates");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:dates", new JavaTypeDescriptor("dates",
                "Date", null));
        field.setMultiple(true);

        PropertyValueProvider provider = new PropertyValueProvider(field, field.getTypeDescriptor(), propertyModel
                .getItemModel());
        for(int i = 0; i < valuesCount; i++) {
            provider.addNew();
        }
        assertEquals(3, provider.size());

        // set dates to the multi-value date field
        JcrPropertyValueModel[] pvms = new JcrPropertyValueModel[valuesCount];
        Iterator<JcrPropertyValueModel> pvmsIter = provider.iterator(0, 3);
        for(int i = 0; i< valuesCount; i++) {
            pvms[i] = pvmsIter.next();
            pvms[i].setObject(dates[i]);
        }
        provider.detach();

        assertEquals(valuesCount, provider.size());
        for(int i = 0; i < valuesCount; i++) {
            assertEquals(dates[i], pvms[i].getObject());
        }
    }

    /**
     * Create a multi-value date property with a null-date value, expected to have a date field with an empty value
     * @throws RepositoryException
     */
    @Test
    public void canReadEmptyValueInMultiValuesDateField() throws RepositoryException {
        final Calendar nullDateCalendar = Calendar.getInstance();
        nullDateCalendar.setTime(PropertyValueProvider.NULL_DATE);

        Node testNode = this.root.addNode(TEST_NODE_NAME, "frontendtest:relaxed");
        testNode.setProperty("frontendtest:dates", nullDateCalendar);
        session.save();

        JcrPropertyModel propertyModel = new JcrPropertyModel(testNode.getPath() + "/frontendtest:dates");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:dates", new JavaTypeDescriptor("date",
                "Date", null));
        field.setMultiple(true);
        PropertyValueProvider provider = new PropertyValueProvider(field, field.getTypeDescriptor(), propertyModel
                .getItemModel());

        assertEquals(1, provider.size());
        JcrPropertyValueModel pvm = provider.iterator(0, 1).next();
        assertEquals(null, pvm.getObject());
        assertFalse(session.hasPendingChanges());
    }

    @Test
    public void testAddedNewDateStoresDate() throws RepositoryException {
        Node testNode = this.root.addNode(TEST_NODE_NAME,"frontendtest:relaxed");
        JcrPropertyModel propertyModel = new JcrPropertyModel(testNode.getPath() + "/frontendtest:date");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:date", new JavaTypeDescriptor("date",
                "Date", null));
        PropertyValueProvider provider = new PropertyValueProvider(field, field.getTypeDescriptor(), propertyModel
                .getItemModel());
        provider.addNew();

        assertEquals(1, provider.size());
        JcrPropertyValueModel pvm = provider.iterator(0, 1).next();
        assertEquals(null, pvm.getObject());
        Date date = new Date();
        pvm.setObject(date);
        provider.detach();

        assertEquals(1, provider.size());
        pvm = provider.iterator(0, 1).next();
        assertEquals(date, pvm.getObject());
    }

    @Test
    public void singleValuedPropertyIsNotModifiedWhenFieldIsMultiple() throws RepositoryException {
        Node test = this.root.addNode(TEST_NODE_NAME, "frontendtest:relaxed");
        test.setProperty("frontendtest:value", "aap");
        session.save();

        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/frontendtest:value");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:value", new JavaTypeDescriptor("string",
                "String", null));
        field.setMultiple(true);

        PropertyValueProvider pvp = new PropertyValueProvider(field, field.getTypeDescriptor(), propModel.getItemModel());
        Iterator<JcrPropertyValueModel> iterator = pvp.iterator(0, 1);
        JcrPropertyValueModel pvm = iterator.next();
        assertEquals("aap", pvm.getObject());

        assertFalse(session.hasPendingChanges());
    }

    @Test
    public void canAddValueToMultiValuedPropertyWithSingleValuedDefinition() throws RepositoryException {
        Node test = this.root.addNode(TEST_NODE_NAME, "frontendtest:relaxed");
        test.setProperty("frontendtest:value", "aap");
        session.save();

        JcrPropertyModel propModel = new JcrPropertyModel(test.getPath() + "/frontendtest:value");
        IFieldDescriptor field = new JavaFieldDescriptor("frontendtest:value", new JavaTypeDescriptor("string",
                "String", null));
        field.setMultiple(true);

        PropertyValueProvider pvp = new PropertyValueProvider(field, field.getTypeDescriptor(), propModel.getItemModel());
        Iterator<JcrPropertyValueModel> iterator = pvp.iterator(0, 1);
        JcrPropertyValueModel pvm = iterator.next();
        assertEquals("aap", pvm.getObject());

        assertFalse(session.hasPendingChanges());

        pvp.addNew();
        assertTrue(session.hasPendingChanges());
    }
}
