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

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class MultiValueGetterImplTest {

    private Value value;
    private ValueGetter<Value, ?> singleValueGetter;
    private Object[] mocks;
    private ValueGetter<Value[], List<?>> multiValueGetter;

    @Before
    public void setUp() {
        singleValueGetter = createMock(ValueGetter.class);
        value = createMock(Value.class);
        mocks = new Object[]{singleValueGetter, value};
        this.multiValueGetter = new MultiValueGetterImpl(singleValueGetter);
    }

    @Test
    public void testGetValuesWithoutElements() throws RepositoryException {
        assertThat(multiValueGetter.getValue(null), is(nullValue()));
        assertThat(multiValueGetter.getValue(new Value[0]), is(nullValue()));
        assertThat(multiValueGetter.getValue(new Value[]{}), is(nullValue()));
    }

    @Test
    public void testGetValues() throws RepositoryException {

        final Value[] values = new Value[]{value, value, value};
        expect(singleValueGetter.getValue(value)).andReturn("X").times(values.length);
        replay(mocks);

        final List<?> result = multiValueGetter.getValue(values);
        assertThat(result.size(), is(values.length));
        for (Object each : result) {
            assertThat(each.toString(), is("X"));
        }
    }
}
