/*
 * Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.util;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class PropertyValueGetterImplTest {
    @Mock
    private Property property;
    @Mock
    private Value value;
    @Mock
    private ValueGetter<Value, Object> singleValueGetter;
    @Mock
    private ValueGetter<Value[], Object> multiValueGetter;

    @Test
    public void testGetValue() throws RepositoryException {

        expect(property.isMultiple()).andReturn(false);
        expect(property.getValue()).andReturn(value);
        expect(singleValueGetter.getValue(value)).andReturn(1L);
        replay(property, singleValueGetter);

        final ValueGetter<Property, Object> propertyValueGetter = new PropertyValueGetterImpl(singleValueGetter, multiValueGetter);
        assertThat(propertyValueGetter.getValue(property), is(1L));
    }

    @Test
    public void testGetValueReturnsNull() throws RepositoryException {

        expect(property.isMultiple()).andReturn(false);
        expect(property.getValue()).andReturn(value);
        expect(singleValueGetter.getValue(value)).andReturn(null);
        replay(property, singleValueGetter);

        final ValueGetter<Property, Object> propertyValueGetter = new PropertyValueGetterImpl(singleValueGetter, multiValueGetter);
        assertThat(propertyValueGetter.getValue(property), is(nullValue()));
    }

    @Test
    public void testGetValues() throws RepositoryException {

        expect(property.isMultiple()).andReturn(true);
        final Value[] values = {value, value, value};
        expect(property.getValues()).andReturn(values);
        List<Long> list = asList(1L, 2L, 3L);
        expect(multiValueGetter.getValue(values)).andReturn(list);
        replay(property, multiValueGetter);

        final ValueGetter<Property, Object> propertyValueGetter = new PropertyValueGetterImpl(singleValueGetter, multiValueGetter);
        assertThat(propertyValueGetter.getValue(property), is(list));
    }

    @Test
    public void testGetValuesWithoutElements() throws RepositoryException {

        expect(property.isMultiple()).andReturn(true);
        expect(property.getValues()).andReturn(null);
        expect(multiValueGetter.getValue(null)).andReturn(null);
        replay(property, multiValueGetter);

        final ValueGetter<Property, Object> propertyValueGetter = new PropertyValueGetterImpl(singleValueGetter, multiValueGetter);
        assertThat(propertyValueGetter.getValue(property), is(nullValue()));
    }

}
