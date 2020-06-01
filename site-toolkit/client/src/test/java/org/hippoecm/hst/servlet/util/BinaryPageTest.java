/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BinaryPageTest {

    @Test
    public void testNotNullContructor() {
        try {
            new BinaryPage(null);
            fail("Null for resource path parameter shouldn't be allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testDefaults() {
        BinaryPage p = new BinaryPage("/my:resource");
        assertEquals("/my:resource", p.getResourcePath());
        assertEquals(HttpServletResponse.SC_NOT_FOUND, p.getStatus());
        assertNull(p.getMimeType());
        assertNull(p.getFileName());
        assertEquals(-1L, p.getLastModified());
        assertEquals(-1L, p.getNextValidityCheckTimeStamp());
        assertEquals(0L, p.getLength());
        assertFalse(p.containsData());
        assertTrue(p.getCreationTime() <= System.currentTimeMillis());
    }

    @Test
    public void testEquality() {

        BinaryPage p1 = new BinaryPage("/my:resource");
        BinaryPage p2 = new BinaryPage("/my:resource");
        assertTrue(p1.equals(p2));
        assertTrue(p1.hashCode() == p2.hashCode());

        p1.setLastModified(1234L);
        assertFalse(p1.equals(p2));
        assertFalse(p1.hashCode() == p2.hashCode());

        p2.setLastModified(1234L);
        assertTrue(p1.equals(p2));
        assertTrue(p1.hashCode() == p2.hashCode());

        p1.setLength(100L);
        assertFalse(p1.equals(p2));
        assertFalse(p1.hashCode() == p2.hashCode());

        p2.setLength(100L);
        assertTrue(p1.equals(p2));
        assertTrue(p1.hashCode() == p2.hashCode());
    }

    @Test
    public void testBinaryData() throws IOException {
        BinaryPage p = new BinaryPage("/my:resource");
        byte[] data = { 'a', 'b', 'c', 'd' };
        p.loadDataFromStream(new ByteArrayInputStream(data));
        assertTrue(p.containsData());
        assertEquals(4, p.getLength());
        InputStream is = p.getStream();
        assertNotNull(is);
        byte b;
        int i;
        int c = 0;
        while ((i = is.read()) != -1) {
            b = (byte) i;
            assertEquals(b, data[c]);
            c++;
        }
        is.close();
    }
}
