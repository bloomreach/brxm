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
package org.hippoecm.repository.util;

import org.junit.Before;
import org.junit.Test;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.math.BigDecimal.ONE;
import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SingleValueGetterImplTest {

    private Value value;
    private Object[] mocks;
    private ValueGetter<Value, ?> singleValueGetter;

    @Before
    public void setUp() {
        value = createMock(Value.class);
        mocks = new Object[]{value};
        this.singleValueGetter = new SingleValueGetterImpl();
    }

    @Test
    public void testGetValue() throws RepositoryException {

        expect(value.getType()).andReturn(PropertyType.LONG);
        expect(value.getLong()).andReturn(1L);
        replay(mocks);

        final Long result = (Long) singleValueGetter.getValue(value);
        assertThat(result, is(1L));
    }

    @Test
    public void testGetNullValueReturnsNull() throws RepositoryException {

        assertThat(singleValueGetter.getValue(null), is(nullValue()));
    }

    @Test
    public void testGetValueForEachSupportedType() throws RepositoryException {

        expect(value.getType()).andReturn(PropertyType.STRING);
        expect(value.getString()).andReturn("test");
        expect(value.getType()).andReturn(PropertyType.NAME);
        expect(value.getString()).andReturn("name");
        expect(value.getType()).andReturn(PropertyType.PATH);
        expect(value.getString()).andReturn("path");
        expect(value.getType()).andReturn(PropertyType.REFERENCE);
        expect(value.getString()).andReturn("reference");
        expect(value.getType()).andReturn(PropertyType.WEAKREFERENCE);
        expect(value.getString()).andReturn("weakreference");

        expect(value.getType()).andReturn(PropertyType.BOOLEAN);
        expect(value.getBoolean()).andReturn(Boolean.FALSE);
        expect(value.getType()).andReturn(PropertyType.DATE);
        expect(value.getDate()).andReturn(new GregorianCalendar());
        expect(value.getType()).andReturn(PropertyType.DECIMAL);
        expect(value.getDecimal()).andReturn(ONE);
        expect(value.getType()).andReturn(PropertyType.DOUBLE);
        expect(value.getDouble()).andReturn(1.234D);
        expect(value.getType()).andReturn(PropertyType.BINARY);
        expect(value.getBinary()).andReturn(createMock(Binary.class));

        replay(mocks);

        final String v1 = singleValueGetter.getValue(value).toString();
        assertThat(v1, is("test"));
        final String v2 = singleValueGetter.getValue(value).toString();
        assertThat(v2, is("name"));
        final String v3 = singleValueGetter.getValue(value).toString();
        assertThat(v3, is("path"));
        final String v4 = singleValueGetter.getValue(value).toString();
        assertThat(v4, is("reference"));
        final String v5 = singleValueGetter.getValue(value).toString();
        assertThat(v5, is("weakreference"));

        final Boolean v7 = Boolean.valueOf(singleValueGetter.getValue(value).toString());
        assertThat(v7, is(Boolean.FALSE));
        assertThat(singleValueGetter.getValue(value), is(Calendar.class));
        assertThat(singleValueGetter.getValue(value), is(BigDecimal.class));
        assertThat(singleValueGetter.getValue(value), is(Double.class));
        assertThat(singleValueGetter.getValue(value), is(Binary.class));
    }

    @Test(expected = RepositoryException.class)
    public void testGetValueThrowsForUndefined() throws RepositoryException {

        expect(value.getType()).andReturn(PropertyType.UNDEFINED);
        replay(mocks);

        singleValueGetter.getValue(value);
    }

}
