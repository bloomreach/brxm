/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.login;

import org.hippoecm.frontend.plugins.login.RememberMeLoginPlugin;
import org.junit.Test;
import static org.junit.Assert.*;

public class Base64EncodingTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    @Test
    public void testEncodeDecode() {
        String original = "admin";
        String encoded = RememberMeLoginPlugin.Base64.encode(original);
        assertEquals("YWRtaW4=", encoded);
        String decoded = RememberMeLoginPlugin.Base64.decode(encoded);
        assertEquals(original, decoded);
    }

    @Test
    public void testPadding() {
        String original = "x";
        String encoded = RememberMeLoginPlugin.Base64.encode(original);
        assertEquals(4, encoded.length());
        assertTrue(encoded.endsWith("=="));
        String decoded = RememberMeLoginPlugin.Base64.decode(encoded);
        assertEquals(original, decoded);

        original = "xx";
        encoded = RememberMeLoginPlugin.Base64.encode(original);
        assertEquals(4, encoded.length());
        assertTrue(encoded.endsWith("="));
        decoded = RememberMeLoginPlugin.Base64.decode(encoded);
        assertEquals(original, decoded);

        original = "xxx";
        encoded = RememberMeLoginPlugin.Base64.encode(original);
        assertEquals(4, encoded.length());
        decoded = RememberMeLoginPlugin.Base64.decode(encoded);
        assertEquals(original, decoded);
    }
}
