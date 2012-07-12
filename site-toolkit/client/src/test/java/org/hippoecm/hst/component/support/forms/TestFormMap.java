/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.component.support.forms;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.junit.Test;

/**
 * TestFormMap
 */
public class TestFormMap {

    @Test
    public void testFieldNamesInOrder() throws Exception {
        String [] originalFieldNames = new String [] { "field1", "field2", "field3" };

        HstRequest request = new MockHstRequest();
        FormMap formMap = new FormMap(request, originalFieldNames);
        String [] fieldNames = formMap.getFieldNames();

        assertNotSame(originalFieldNames, fieldNames);
        assertTrue(ArrayUtils.isEquals(originalFieldNames, fieldNames));

        Map<String, FormField> map = formMap.getFormMap();
        String [] keys = map.keySet().toArray(new String[map.size()]);

        assertTrue(ArrayUtils.isEquals(originalFieldNames, keys));
    }

}
