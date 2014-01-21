/*
 * Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Before;
import org.junit.Test;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.List;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PropertyValueGetterImplTest {

    private Property property;
    private Value value;
    private ValueGetter<Value, ?> singleValueGetter;
    private ValueGetter<Value[], ?> multiValueGetter;
    private Object[] mocks;
    private ValueGetter<Property, ?> propertyValueGetter;

    @Before
    public void setUp() {
        property = createMock(Property.class);
        value = createMock(Value.class);
        singleValueGetter = createMock(ValueGetter.class);
        multiValueGetter = createMock(ValueGetter.class);
        mocks = new Object[]{property, value, singleValueGetter, multiValueGetter};
        this.propertyValueGetter = new PropertyValueGetterImpl(singleValueGetter, multiValueGetter);
    }

    @Test
    public void testGetValue() throws RepositoryException {

        expect(property.isMultiple()).andReturn(false);
        expect(property.getValue()).andReturn(value);
        expect(singleValueGetter.getValue(value)).andReturn(1L);
        replay(mocks);

        final Long result = (Long) propertyValueGetter.getValue(property);
        assertThat(result, is(1L));
    }

    @Test
    public void testGetValueReturnsNull() throws RepositoryException {

        expect(property.isMultiple()).andReturn(false);
        expect(property.getValue()).andReturn(value);
        expect(singleValueGetter.getValue(value)).andReturn(null);
        replay(mocks);

        assertThat(propertyValueGetter.getValue(property), is(nullValue()));
    }

    @Test
    public void testGetValues() throws RepositoryException {

        expect(property.isMultiple()).andReturn(true);
        final Value[] values = {value, value, value};
        expect(property.getValues()).andReturn(values);
        List<Long> list = asList(1L, 2L, 3L);
        expect(multiValueGetter.getValue(values)).andReturn(list);
        replay(mocks);

        final List<Long> result = (List<Long>) propertyValueGetter.getValue(property);
        assertThat(result, is(list));
    }

    @Test
    public void testGetValuesWithoutElements() throws RepositoryException {

        expect(property.isMultiple()).andReturn(true);
        expect(property.getValues()).andReturn(null);
        expect(multiValueGetter.getValue(null)).andReturn(null);
        replay(mocks);

        assertThat(propertyValueGetter.getValue(property), is(nullValue()));
    }

}
