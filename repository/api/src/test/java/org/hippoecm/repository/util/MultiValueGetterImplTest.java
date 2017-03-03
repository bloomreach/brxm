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

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class MultiValueGetterImplTest {

    @Mock
    private Value value;
    @Mock
    private ValueGetter<Value, Object> singleValueGetter;

    @Test
    public void testGetValuesWithoutElements() throws RepositoryException {
        final ValueGetter<Value[], List<?>> multiValueGetter = new MultiValueGetterImpl(singleValueGetter);
        assertThat(multiValueGetter.getValue(null), is(nullValue()));
        assertThat(multiValueGetter.getValue(new Value[0]), is(nullValue()));
        assertThat(multiValueGetter.getValue(new Value[]{}), is(nullValue()));
    }

    @Test
    public void testGetValues() throws RepositoryException {

        final Value[] values = new Value[]{value, value, value};
        expect(singleValueGetter.getValue(value)).andReturn("X").times(values.length);
        replay(singleValueGetter);

        final ValueGetter<Value[], List<?>> multiValueGetter = new MultiValueGetterImpl(singleValueGetter);
        final List<?> result = multiValueGetter.getValue(values);
        assertThat(result.size(), is(values.length));
        for (Object each : result) {
            assertThat(each.toString(), is("X"));
        }
    }
}
