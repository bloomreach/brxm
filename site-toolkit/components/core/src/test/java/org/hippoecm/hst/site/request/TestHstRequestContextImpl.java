/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import java.util.Map;

import org.apache.commons.collections.EnumerationUtils;
import org.junit.Test;

import com.google.common.collect.Iterables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TestHstRequestContextImpl
 * @version $Id$
 */
public class TestHstRequestContextImpl {

    @Test
    public void testModelAndAttributes() {
        HstRequestContextImpl requestContext = new HstRequestContextImpl(null);
        assertTrue(requestContext.attributes == null || requestContext.attributes.isEmpty());

        final Map<String, Object> attributes = requestContext.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());

        try {
            attributes.put("attr1", "value1");
            fail("Must not allow to add attribute directly from the attributes map.");
        } catch (UnsupportedOperationException expected) {
        }

        requestContext.setModel("model1", "modelValue1");
        assertEquals("modelValue1", requestContext.getAttribute("model1"));

        requestContext.setAttribute("attr1", "value1");
        assertEquals("value1", requestContext.getAttribute("attr1"));
        assertEquals("modelValue1", requestContext.getAttribute("model1"));
        assertNull(requestContext.getModel("attr1"));

        assertEquals("value1", requestContext.getAttributes().get("attr1"));
        assertEquals("modelValue1", requestContext.getAttributes().get("model1"));

        assertTrue(Iterables.contains(requestContext.getModelNames(), "model1"));
        assertFalse(Iterables.contains(requestContext.getModelNames(), "attr1"));
        assertTrue(EnumerationUtils.toList(requestContext.getAttributeNames()).contains("attr1"));
        assertTrue(EnumerationUtils.toList(requestContext.getAttributeNames()).contains("model1"));

        assertEquals("value1", requestContext.getAttributes().get("attr1"));
        assertFalse(requestContext.attributes.isEmpty());
        assertFalse(requestContext.getAttributes().isEmpty());

        requestContext.removeModel("model1");
        assertNull(requestContext.getModel("model1"));
        assertNull(requestContext.getModelsMap().get("model1"));
        assertTrue(requestContext.getModelsMap().isEmpty());

        requestContext.removeAttribute("attr1");
        requestContext.removeAttribute("model1");
        assertNull(requestContext.getAttribute("attr1"));
        assertNull(requestContext.getAttributes().get("attr1"));
        assertTrue(requestContext.attributes.isEmpty());
        assertTrue(requestContext.getAttributes().isEmpty());
    }
    
}
