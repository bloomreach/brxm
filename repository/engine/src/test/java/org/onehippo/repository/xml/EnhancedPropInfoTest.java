/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.xml;

import javax.jcr.Value;

import org.apache.jackrabbit.core.xml.TextValue;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValueFactory;

import static org.junit.Assert.assertEquals;

public class EnhancedPropInfoTest {

    @Test
    public void testNewValuesFromAppend() throws Exception {
        final MockNode root = MockNode.root();
        root.setProperty("test", new String[] { "0" });

        final MockValueFactory valueFactory = root.getSession().getValueFactory();
        Value[] appendValues = { valueFactory.createValue("1") };

        EnhancedPropInfo insertPropInfo = new EnhancedPropInfo(new TextValue[1], "append", null, true);
        Value[] newValues = insertPropInfo.calculateNewValues(appendValues, root.getProperty("test").getValues());
        assertEquals(2, newValues.length);
        assertEquals("0", newValues[0].getString());
        assertEquals("1", newValues[1].getString());

    }

    @Test
    public void testNewValuesFromInsert() throws Exception {
        final MockNode root = MockNode.root();
        root.setProperty("test", new String[] { "0", "2" });

        final MockValueFactory valueFactory = root.getSession().getValueFactory();
        Value[] insertValues = { valueFactory.createValue("1") };

        // insert in the middle
        EnhancedPropInfo insertPropInfo = new EnhancedPropInfo(new TextValue[1], "insert", "1", true);
        Value[] newValues = insertPropInfo.calculateNewValues(insertValues, root.getProperty("test").getValues());
        assertEquals(3, newValues.length);
        assertEquals("0", newValues[0].getString());
        assertEquals("1", newValues[1].getString());
        assertEquals("2", newValues[2].getString());

        // insert at the start
        root.setProperty("test", new String[] { "2" });
        insertPropInfo = new EnhancedPropInfo(new TextValue[1], "insert", "0", true);
        insertValues = new Value[] { valueFactory.createValue("0"), valueFactory.createValue("1") };
        newValues = insertPropInfo.calculateNewValues(insertValues, root.getProperty("test").getValues());
        assertEquals(3, newValues.length);
        assertEquals("0", newValues[0].getString());
        assertEquals("1", newValues[1].getString());
        assertEquals("2", newValues[2].getString());

        // 'insert' at the end
        root.setProperty("test", new String[] { "0" });
        insertPropInfo = new EnhancedPropInfo(new TextValue[1], "insert", "1", true);
        insertValues = new Value[] { valueFactory.createValue("1"), valueFactory.createValue("2") };
        newValues = insertPropInfo.calculateNewValues(insertValues, root.getProperty("test").getValues());
        assertEquals(3, newValues.length);
        assertEquals("0", newValues[0].getString());
        assertEquals("1", newValues[1].getString());
        assertEquals("2", newValues[2].getString());
    }
}
