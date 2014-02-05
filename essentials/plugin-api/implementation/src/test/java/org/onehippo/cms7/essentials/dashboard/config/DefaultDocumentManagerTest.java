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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class DefaultDocumentManagerTest extends BaseRepositoryTest {


    @Test
    public void testTypes() throws Exception {

        final DocumentManager manager = new DefaultDocumentManager(getContext());
        final String parentPath = "/foo/bar";
        TestBeanDocument document = new TestBeanDocument("testDoc", parentPath);
        document.setBooleanVal(true);
        final List<Boolean> booleanValArray = new ArrayList<>();
        booleanValArray.add(true);
        document.setBooleanValArray(booleanValArray);
        document.setDoubleCounter(1D);
        final List<Double> doubleCounterArray = new ArrayList<>();
        doubleCounterArray.add(1D);
        document.setDoubleCounterArray(doubleCounterArray);
        document.setIntCounter(1);
        final List<Integer> intCounterArray = new ArrayList<>();
        intCounterArray.add(1);
        document.setIntCounterArray(intCounterArray);
        document.setLongCounter(1L);
        final List<Long> longCounterArray = new ArrayList<>();
        longCounterArray.add(1L);
        document.setLongCounterArray(longCounterArray);
        document.setStringType("string");
        final List<String> stringTypeArray = new ArrayList<>();
        stringTypeArray.add("string");
        document.setStringTypeArray(stringTypeArray);
        final Calendar today = Calendar.getInstance();
        document.setDateType(today);
        final List<Calendar> dateTypeArray = new ArrayList<>();
        dateTypeArray.add(today);
        document.setDateTypeArray(dateTypeArray);
        final boolean saved = manager.saveDocument(document);
        assertTrue("Expected document to be saved", saved);
        TestBeanDocument fetched = manager.fetchDocument(document.getPath(), TestBeanDocument.class);
        assertEquals("testDoc", fetched.getName());
        assertTrue(fetched.isBooleanVal());
        assertTrue(fetched.getBooleanValArray().size() == 1);
        assertTrue(1D == fetched.getDoubleCounter());
        assertTrue(fetched.getDoubleCounterArray().size() == 1);
        assertTrue(1 == fetched.getIntCounter());
        assertTrue(fetched.getIntCounterArray().size() == 1);
        assertTrue(1L == fetched.getLongCounter());
        assertTrue(fetched.getLongCounterArray().size() == 1);
        assertEquals("string", fetched.getStringType());
        assertTrue(fetched.getStringTypeArray().size() == 1);
        assertEquals(today.get(Calendar.DAY_OF_MONTH), fetched.getDateType().get(Calendar.DAY_OF_MONTH));
        assertTrue(fetched.getDateTypeArray().size() == 1);

    }

    @Test
    public void testSaveDocument() throws Exception {


        final DocumentManager manager = new DefaultDocumentManager(getContext());
        final String parentPath = "/foo/bar";
        Document document = new BaseDocument("myConfig", parentPath);
        document.addProperty("foo");
        document.addProperty("bar");
        document.addProperty("foobar");
        final boolean saved = manager.saveDocument(document);
        assertTrue("Expected document to be saved", saved);
        Document fetched = manager.fetchDocument(document.getPath(), BaseDocument.class);
        assertEquals("myConfig", fetched.getName());
        assertEquals("/foo/bar", fetched.getParentPath());
        assertEquals("/foo/bar/myConfig", fetched.getPath());
        assertEquals(fetched.getProperties().get(0), "foo");
        assertEquals(fetched.getProperties().get(1), "bar");
        assertEquals(fetched.getProperties().get(2), "foobar");
        // save as class
        final String classPath = BaseDocument.class.getName();
        document = new BaseDocument(BaseDocument.class.getSimpleName(), GlobalUtils.getParentConfigPath(classPath));
        document.addProperty("foo");
        document.addProperty("bar");
        document.addProperty("foobar");
        manager.saveDocument(document);
        fetched = manager.fetchDocument(classPath);
        assertEquals(fetched.getProperties().get(0), "foo");
        assertEquals(fetched.getProperties().get(1), "bar");
        assertEquals(fetched.getProperties().get(2), "foobar");

    }
}
