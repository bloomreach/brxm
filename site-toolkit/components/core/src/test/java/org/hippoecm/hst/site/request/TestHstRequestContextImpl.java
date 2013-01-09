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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

/**
 * TestHstRequestContextImpl
 * @version $Id$
 */
public class TestHstRequestContextImpl {
    
    @Test
    public void testAttributes() {
        HstRequestContextImpl requestContext = new HstRequestContextImpl(null);
        assertNull(requestContext.attributes);
        
        Map<String, Object> attributes = requestContext.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
        
        try {
            attributes.put("new1", "value1");
            fail("Must not allow to add attribute directly from the attributes map.");
        } catch (UnsupportedOperationException expected) {
        }
        
        requestContext.setAttribute("new1", "value1");
        attributes = requestContext.getAttributes();
        assertEquals("value1", requestContext.getAttribute("new1"));
        assertEquals("value1", attributes.get("new1"));
        assertNotNull(requestContext.attributes);
        assertFalse(requestContext.attributes.isEmpty());
        assertFalse(attributes.isEmpty());
        
        requestContext.removeAttribute("new1");
        attributes = requestContext.getAttributes();
        assertNull(requestContext.getAttribute("new1"));
        assertNull(attributes.get("new1"));
        assertTrue(requestContext.attributes.isEmpty());
        assertTrue(attributes.isEmpty());
    }
    
}
