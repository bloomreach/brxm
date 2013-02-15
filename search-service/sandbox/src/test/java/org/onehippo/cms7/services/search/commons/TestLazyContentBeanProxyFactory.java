/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.commons;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cms7.services.search.binder.PropertyValueProvider;
import org.onehippo.cms7.services.search.commons.Bar;

public class TestLazyContentBeanProxyFactory {

    @Test
    public void testProxyingFooInterface() {

        Map<String, Object> propMap = new HashMap<String, Object>();
        propMap.put("id", "123");
        propMap.put("message", "foo's message");
        propMap.put("active", Boolean.TRUE);
        PropertyValueProvider pvp = new MapPropertyValueProvider(propMap);

        Foo foo = LazyContentBeanProxyFactory.getProxy(new SimpleFoo(), pvp, Foo.class);

        assertEquals("123", foo.getId());
        assertEquals("foo's message", foo.getMessage());
        assertEquals(Boolean.TRUE, foo.isActive());
    }

    @Test
    public void testProxyingBarClass() {

        Map<String, Object> propMap = new HashMap<String, Object>();
        propMap.put("id", "123");
        propMap.put("message", "bar's message");
        propMap.put("active", Boolean.TRUE);
        PropertyValueProvider pvp = new MapPropertyValueProvider(propMap);

        Bar bar = LazyContentBeanProxyFactory.getProxy(new Bar(), pvp);

        assertEquals("123", bar.getId());
        assertEquals("bar's message", bar.getMessage());
        assertEquals(Boolean.TRUE, bar.isActive());
    }

}

